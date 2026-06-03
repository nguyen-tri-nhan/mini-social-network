#!/bin/bash
awslocal s3 mb s3://social-images
awslocal s3api put-bucket-acl --bucket social-images --acl public-read
echo "LocalStack S3 bucket 'social-images' created"
