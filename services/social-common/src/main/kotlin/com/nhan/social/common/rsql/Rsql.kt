package com.nhan.social.common.rsql

import cz.jirutka.rsql.parser.RSQLParser
import cz.jirutka.rsql.parser.RSQLParserException
import cz.jirutka.rsql.parser.ast.*

/**
 * Result of parsing an RSQL expression into HQL + named params.
 * Pass [hql] and [params] directly to Panache find()/count().
 */
data class RsqlQuerySpec(
    val hql: String,
    val params: Map<String, Any>,
) {
    companion object {
        fun of(baseCondition: String) = RsqlQuerySpec(baseCondition, emptyMap())
    }
}

/** Sort descriptor returned by [parseRsqlSort]. Convert to framework Sort in each service. */
data class SortSpec(val field: String, val ascending: Boolean)

/**
 * Describes one filterable field.
 * @param hqlName HQL field name — defaults to the map key if null.
 * @param convert Converts the raw string argument to the correct JVM type.
 */
data class RsqlField(
    val hqlName: String? = null,
    val convert: (String) -> Any,
)

/**
 * Parses an RSQL string into an [RsqlQuerySpec] ready for Panache.
 *
 * @param rsql       RSQL expression, e.g. `"authorId==uuid;createdAt=gt=2026-01-01T00:00:00Z"`
 * @param fields     Whitelist of filterable API fields. Keys are the field names clients use.
 * @param baseCondition  HQL fragment always ANDed with the parsed filter (e.g. `"visible = true"`).
 * @throws IllegalArgumentException on unknown field, bad syntax, or unsupported operator.
 */
fun parseRsql(
    rsql: String,
    fields: Map<String, RsqlField>,
    baseCondition: String = "1=1",
): RsqlQuerySpec {
    val node = try {
        RSQLParser().parse(rsql)
    } catch (e: RSQLParserException) {
        throw IllegalArgumentException("Invalid filter syntax: ${e.message}")
    }
    val inner = node.accept(GenericRsqlVisitor(fields), Unit)
    return RsqlQuerySpec("$baseCondition and (${inner.hql})", inner.params)
}

/**
 * Parses `?sort=field,asc` / `?sort=field,desc` into a [SortSpec].
 *
 * @param sort         Raw query param value, e.g. `"voteCount,desc"`. Null → use defaults.
 * @param allowedFields Whitelist of sortable field names.
 * @param defaultField  Field used when [sort] is null.
 * @param defaultAscending Sort direction used when [sort] is null (false = DESC).
 * @throws IllegalArgumentException when the field is not in [allowedFields].
 */
fun parseRsqlSort(
    sort: String?,
    allowedFields: Set<String>,
    defaultField: String,
    defaultAscending: Boolean = false,
): SortSpec {
    sort ?: return SortSpec(defaultField, defaultAscending)
    val parts = sort.split(",")
    val field = parts[0].trim()
    if (field !in allowedFields) throw IllegalArgumentException("Sort by '$field' is not allowed")
    val ascending = parts.getOrElse(1) { "desc" }.trim().lowercase() == "asc"
    return SortSpec(field, ascending)
}

// ── Internal visitor ──────────────────────────────────────────────────────────

private class GenericRsqlVisitor(
    private val fields: Map<String, RsqlField>,
) : RSQLVisitor<RsqlQuerySpec, Unit> {

    private var idx = 0

    override fun visit(node: AndNode, param: Unit): RsqlQuerySpec =
        node.children
            .map { it.accept(this, param) }
            .reduce { a, b -> RsqlQuerySpec("(${a.hql} and ${b.hql})", a.params + b.params) }

    override fun visit(node: OrNode, param: Unit): RsqlQuerySpec =
        node.children
            .map { it.accept(this, param) }
            .reduce { a, b -> RsqlQuerySpec("(${a.hql} or ${b.hql})", a.params + b.params) }

    override fun visit(node: ComparisonNode, param: Unit): RsqlQuerySpec {
        val apiField = node.selector
        val fieldDef = fields[apiField]
            ?: throw IllegalArgumentException("Filtering by '$apiField' is not allowed")
        val hqlField = fieldDef.hqlName ?: apiField
        val op = node.operator

        if (op.isMultiValue) {
            val keyword = if (op.symbols.contains("=in=")) "in" else "not in"
            val entries = node.arguments.mapIndexed { i, raw ->
                "p${idx}_$i" to fieldDef.convert(raw)
            }
            idx++
            val clause = entries.joinToString(", ") { ":${it.first}" }
            return RsqlQuerySpec("$hqlField $keyword ($clause)", entries.toMap())
        }

        val pName = "p${idx++}"
        val value = fieldDef.convert(node.arguments.first())
        val hqlOp = when {
            op.symbols.contains("==")   -> "="
            op.symbols.contains("!=")   -> "!="
            op.symbols.contains("=gt=") -> ">"
            op.symbols.contains("=ge=") -> ">="
            op.symbols.contains("=lt=") -> "<"
            op.symbols.contains("=le=") -> "<="
            else -> throw IllegalArgumentException("Operator '${op.symbols.first()}' is not supported")
        }
        return RsqlQuerySpec("$hqlField $hqlOp :$pName", mapOf(pName to value))
    }
}
