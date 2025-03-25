#!/bin/sh
# Elasticsearch 캐시 정리
curl -X POST "elasticsearch:9200/_cache/clear"

# Redis 캐시 정리 (사용 패턴에 따라 필요한 경우)
redis-cli -h redis FLUSHDB

# STT/TTS 임시 파일 정리
curl -X POST "stt_tts:5000/cleanup/temp"