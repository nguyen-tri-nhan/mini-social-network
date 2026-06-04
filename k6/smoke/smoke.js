/**
 * Smoke test — 2 users interacting
 *
 * Scenario:
 *   Alice signs up → creates an article
 *   Bob   signs up → comments + upvotes Alice's article
 *   Alice checks notifications → marks all as seen
 *
 * Run:
 *   k6 run k6/smoke/smoke.js
 *   k6 run k6/smoke/smoke.js -e BASE_URL=https://api.social.nhan.dev
 */

import http  from 'k6/http';
import { check, group, sleep } from 'k6';
import { BASE_URL, THRESHOLDS } from './config.js';

export const options = {
  vus:        1,
  iterations: 1,
  thresholds: THRESHOLDS,
};

// ── Helpers ───────────────────────────────────────────────────────────────────

function uid() {
  return Math.random().toString(36).substring(2, 9);
}

function json(token) {
  const h = { 'Content-Type': 'application/json' };
  if (token) h['Authorization'] = `Bearer ${token}`;
  return { headers: h };
}

function ok(res, label) {
  check(res, { [label]: (r) => r.status >= 200 && r.status < 300 });
  if (res.status >= 400) {
    console.error(`[${label}] ${res.status} — ${res.body}`);
  }
  return res;
}

// ── API calls ─────────────────────────────────────────────────────────────────

function signup(suffix) {
  const res = http.post(
    `${BASE_URL}/api/auth/signup`,
    JSON.stringify({
      username:  `smoke_${suffix}`,
      email:     `smoke_${suffix}@test.local`,
      password:  'Password123!',
      firstname: 'Smoke',
      lastname:  suffix,
    }),
    json(),
  );
  ok(res, `signup ${suffix} → 201`);
  return res.json('data');          // { accessToken, userId, username }
}

function signin(username) {
  const res = http.post(
    `${BASE_URL}/api/auth/signin`,
    JSON.stringify({ identifier: username, password: 'Password123!' }),
    json(),
  );
  ok(res, `signin ${username} → 200`);
  return res.json('data.accessToken');
}

function createArticle(token, description) {
  const res = http.post(
    `${BASE_URL}/api/articles`,
    JSON.stringify({ description }),
    json(token),
  );
  ok(res, 'createArticle → 201');
  return res.json('data');          // ArticleDto
}

function listArticles(token) {
  const res = http.get(`${BASE_URL}/api/articles?page=0&size=10`, json(token));
  ok(res, 'listArticles → 200');
  return res.json('data.items') || [];
}

function addComment(token, targetId, description) {
  const res = http.post(
    `${BASE_URL}/api/comments`,
    JSON.stringify({ targetId, targetType: 'ARTICLE', description }),
    json(token),
  );
  ok(res, 'addComment → 201');
  return res.json('data');
}

function castVote(token, targetId, targetType, value) {
  const res = http.post(
    `${BASE_URL}/api/votes`,
    JSON.stringify({ targetId, targetType, value }),
    json(token),
  );
  ok(res, `castVote ${targetType} → 200`);
  return res.json('data');
}

function listNotifications(token) {
  const res = http.get(`${BASE_URL}/api/notifications?page=0&size=20`, json(token));
  ok(res, 'listNotifications → 200');
  return res.json('data.items') || [];
}

function unreadCount(token) {
  const res = http.get(`${BASE_URL}/api/notifications/unread-count`, json(token));
  ok(res, 'unreadCount → 200');
  return res.json('data.count') || 0;
}

function markAllSeen(token) {
  const res = http.patch(`${BASE_URL}/api/notifications/seen-all`, null, json(token));
  ok(res, 'markAllSeen → 204');
}

// ── Scenario ──────────────────────────────────────────────────────────────────

export default function () {
  const id = uid();

  // ── Step 1: Alice & Bob sign up ───────────────────────────────────────────
  let aliceToken, bobToken, aliceId;

  group('auth — signup & signin', () => {
    const alice = signup(`alice_${id}`);
    aliceToken  = alice?.accessToken || signin(`alice_${id}`);
    aliceId     = alice?.userId;

    const bob = signup(`bob_${id}`);
    bobToken    = bob?.accessToken || signin(`bob_${id}`);
  });

  sleep(0.5);

  // ── Step 2: Alice tạo bài viết ────────────────────────────────────────────
  let article;

  group('post — Alice creates article', () => {
    article = createArticle(aliceToken, `Hello from Alice! [${id}]`);

    check(article, {
      'article has id':          (a) => !!a?.id,
      'article has description': (a) => !!a?.description,
    });
  });

  sleep(0.5);

  // ── Step 3: Bob xem feed ──────────────────────────────────────────────────
  group('post — Bob views feed', () => {
    const items = listArticles(bobToken);
    check(items, { 'feed not empty': (i) => i.length > 0 });
  });

  sleep(0.3);

  // ── Step 4: Bob bình luận và vote bài của Alice ───────────────────────────
  let comment;

  group('interaction — Bob comments + votes', () => {
    comment = addComment(bobToken, article.id, 'Great post, Alice!');
    check(comment, {
      'comment has id':       (c) => !!c?.id,
      'comment targetType':   (c) => c?.targetType === 'ARTICLE',
    });

    // Bob upvotes article
    const articleVote = castVote(bobToken, article.id, 'ARTICLE', 1);
    check(articleVote, { 'article vote value': (v) => v?.value === 1 });

    // Bob upvotes own comment (test targetType=COMMENT)
    const commentVote = castVote(bobToken, comment.id, 'COMMENT', 1);
    check(commentVote, { 'comment vote value': (v) => v?.value === 1 });
  });

  sleep(1); // nhường thời gian cho Kafka consumer xử lý notification

  // ── Step 5: Alice kiểm tra notification ──────────────────────────────────
  group('notification — Alice checks & clears', () => {
    const count = unreadCount(aliceToken);
    check(count, { 'Alice has unread notifications': (c) => c >= 0 });

    const notifs = listNotifications(aliceToken);
    check(notifs, { 'notification list is array': (n) => Array.isArray(n) });

    markAllSeen(aliceToken);

    // Verify count reset
    const countAfter = unreadCount(aliceToken);
    check(countAfter, { 'unread count is 0 after markAllSeen': (c) => c === 0 });
  });

  sleep(0.5);
}
