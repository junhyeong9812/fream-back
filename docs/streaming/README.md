# 동영상 스트리밍 서비스 구현 가이드

## 📚 목차

1. [전체 시스템 아키텍처](#전체-시스템-아키텍처)
2. [주요 스트리밍 기술](#주요-스트리밍-기술)
3. [서버 컴포넌트 상세 설명](#서버-컴포넌트-상세-설명)
4. [구현 기술 스택](#구현-기술-스택)
5. [시스템 최적화 방안](#시스템-최적화-방안)
6. [중요 라이브러리 설명](#중요-라이브러리-설명)

---

## 1. 전체 시스템 아키텍처

### 실시간 방송 플로우
```
[방송자] → RTMP/WebRTC → [Ingest Server] → FFmpeg → [HLS Segment Server] → CDN → [시청자]
                                                                     ↓
                                                                [채팅 서버] ↔ [시청자]
```

### VOD 스트리밍 플로우
```
[VOD 서버] → CDN/Spring 서버 → [시청자]
      ↓
[채팅 서버] ↔ [시청자]
```

---

## 2. 주요 스트리밍 기술

### 실시간 스트리밍 프로토콜

| 기술 | 지연시간 | 특징 | 사용 사례 |
|------|----------|------|-----------|
| **WebRTC** | 100-300ms | P2P 양방향 통신 | 화상 통화, 실시간 게임 |
| **HLS** | 15-30초 | 세그먼트 분할, 적응형 | 라이브 방송, 스포츠 중계 |
| **RTMP** | 2-5초 | 낮은 지연시간 | 실시간 인터랙션 필요한 방송 |
| **DASH** | 10-20초 | 세밀한 품질 제어 | 고품질 VOD 서비스 |

### VOD 스트리밍 방식

| 방식 | 구현 난이도 | 장점 | 단점 |
|------|------------|------|------|
| **Progressive Download** | 쉬움 | 간단한 구현 | 전체 다운로드 필요 |
| **Adaptive Streaming** | 보통 | 네트워크 최적화 | 초기 세팅 복잡 |
| **CDN 기반** | 복잡 | 글로벌 확장성 | 비용 상승 |

---

## 3. 서버 컴포넌트 상세 설명

### A. Ingest Server
**역할:** 방송자의 스트림을 수신하는 입구점

**주요 기능:**
- RTMP 프로토콜 처리
- WebRTC 시그널링 처리
- 스트림 인증 및 검증

**구현 옵션:**
```java
// Spring Boot RTMP 수신 예제
@Controller
public class RTMPController {
    @PostMapping("/publish")
    public ResponseEntity<String> handleRTMPStream() {
        // RTMP 스트림 처리 로직
        return ResponseEntity.ok("Stream received");
    }
}
```

### B. FFmpeg 트랜스코더
**역할:** 입력 스트림을 다양한 형식/화질로 변환

**주요 작업:**
- 비디오 해상도 변환 (1080p → 720p → 480p)
- 비트레이트 조절
- HLS 세그먼트 생성
- 코덱 변환 (H.264, H.265)

**CLI 명령어 예제:**
```bash
ffmpeg -i input.mp4 \
  -c:v libx264 -c:a aac \
  -hls_time 6 -hls_list_size 0 \
  -hls_segment_filename "segment_%03d.ts" \
  -b:v:0 800k -s:v:0 640x360 \
  -b:v:1 1200k -s:v:1 960x540 \
  -b:v:2 2400k -s:v:2 1280x720 \
  output.m3u8
```

### C. HLS Segment Server
**역할:** 변환된 세그먼트 파일들을 서빙

**구현:**
```javascript
// React HLS.js 플레이어
import Hls from 'hls.js';

const HLSPlayer = ({ url }) => {
  const videoRef = useRef();
  
  useEffect(() => {
    const hls = new Hls();
    hls.loadSource(url);
    hls.attachMedia(videoRef.current);
    
    hls.on(Hls.Events.MANIFEST_PARSED, () => {
      video.play();
    });
  }, [url]);
  
  return <video ref={videoRef} controls />;
};
```

### D. 채팅 서버
**역할:** 실시간 채팅 메시지 처리

**구현 방식:**
```java
// WebSocket 채팅 서버
@ServerEndpoint("/chat/{roomId}")
public class ChatEndpoint {
    private static Map<String, Session> sessions = new ConcurrentHashMap<>();
    
    @OnMessage
    public String onMessage(String message, @PathVariable String roomId) {
        // 메시지 브로드캐스트
        sessions.values().forEach(session -> {
            session.getAsyncRemote().sendText(message);
        });
        return message;
    }
}
```

---

## 4. 구현 기술 스택

### 백엔드 (Spring Boot)

| 컴포넌트 | 기술 | 용도 |
|----------|------|------|
| 스트리밍 서버 | Spring WebFlux | 비동기 스트림 처리 |
| 채팅 서버 | WebSocket | 실시간 양방향 통신 |
| 인코딩 | FFmpeg | 비디오 변환 처리 |
| 캐싱 | Redis | 세그먼트 캐싱 |
| 메시지 큐 | Kafka | 이벤트 처리 |

### 프론트엔드 (React)

| 기능 | 라이브러리 | 특징 |
|------|------------|------|
| HLS 재생 | hls.js | 브라우저 호환성 |
| 비디오 플레이어 | Video.js | 확장성과 커스터마이징 |
| WebRTC | PeerJS | P2P 통신 간소화 |
| 채팅 | Socket.IO | 실시간 메시징 |

---

## 5. 시스템 최적화 방안

### 실시간 스트리밍 최적화

#### 1. RTMP → HLS 변환 최적화
```bash
# 최적화된 FFmpeg 명령어
ffmpeg -i rtmp://localhost/live/stream \
  -c:v libx264 -preset ultrafast \
  -crf 28 -g 60 \
  -f hls -hls_time 4 \
  -hls_allow_cache 1 \
  -hls_flags delete_segments+append_list \
  output.m3u8
```

#### 2. WebRTC SFU 구조
- **mediasoup**: 고성능 SFU 서버
- **Kurento**: WebRTC 미디어 서버
- **Jitsi**: 오픈소스 화상회의 솔루션

### VOD 최적화

#### 1. 미리 인코딩된 HLS 생성
```javascript
// 다중 해상도 생성 자동화
const transcodeToHLS = async (inputFile) => {
  const resolutions = [
    { height: 360, bitrate: '800k' },
    { height: 720, bitrate: '2500k' },
    { height: 1080, bitrate: '5000k' }
  ];
  
  for (const res of resolutions) {
    await exec(`ffmpeg -i ${inputFile} 
      -vf scale=-1:${res.height} 
      -b:v ${res.bitrate} 
      -hls_time 10 
      variant_${res.height}p.m3u8`);
  }
};
```

#### 2. CDN 캐싱 전략
```nginx
# Nginx 캐싱 설정
location /hls/ {
    add_header Cache-Control "public, max-age=3600";
    add_header Access-Control-Allow-Origin "*";
    add_header Access-Control-Allow-Methods "GET";
    
    expires 1h;
    proxy_cache_valid 200 302 24h;
}
```

### 채팅 시스템 최적화

#### 1. Redis Pub/Sub 구조
```java
// Redis Pub/Sub 채팅 구현
@Service
public class ChatService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    public void publishMessage(String channel, String message) {
        redisTemplate.convertAndSend(channel, message);
    }
    
    public void subscribeToChannel(String channel) {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.subscribe((message, pattern) -> {
                handleIncomingMessage(new String(message.getBody()));
            }, channel.getBytes());
            return null;
        });
    }
}
```

#### 2. 메시지 배치 처리
```javascript
// 클라이언트 메시지 배치
const useChatBatch = (socket) => {
  const [messageBuffer, setMessageBuffer] = useState([]);
  
  useEffect(() => {
    const interval = setInterval(() => {
      if (messageBuffer.length > 0) {
        socket.emit('batch_messages', messageBuffer);
        setMessageBuffer([]);
      }
    }, 100);
    
    return () => clearInterval(interval);
  }, [messageBuffer]);
};
```

---

## 6. 중요 라이브러리 설명

### FFmpeg
**개념:** 오픈소스 멀티미디어 프레임워크  
**주요 기능:**
- 비디오/오디오 인코딩/디코딩
- 스트리밍 프로토콜 지원
- 필터링 및 변환

**사용 예시:**
```bash
# RTMP 스트림을 HLS로 변환
ffmpeg -i rtmp://server/live/stream \
  -c:v libx264 -c:a aac \
  -hls_time 6 -hls_playlist_type event \
  -f hls output.m3u8
```

### Redis
**개념:** 인메모리 데이터 구조 저장소  
**채팅에서의 역할:**
- 실시간 메시지 Pub/Sub
- 채팅방 참여자 관리
- 메시지 임시 캐싱

### WebRTC
**개념:** 브라우저 간 P2P 통신 기술  
**구성요소:**
1. **RTCPeerConnection**: 피어 연결 관리
2. **MediaStream**: 미디어 데이터 처리
3. **RTCDataChannel**: 데이터 통신 채널

**시그널링 예제:**
```javascript
// WebRTC 시그널링 과정
const peer = new RTCPeerConnection();

// 로컬 스트림 추가
const localStream = await navigator.mediaDevices.getUserMedia({video: true, audio: true});
localStream.getTracks().forEach(track => peer.addTrack(track, localStream));

// Offer 생성
const offer = await peer.createOffer();
await peer.setLocalDescription(offer);

// 시그널링 서버로 Offer 전송
socket.emit('offer', offer);
```

### SFU (Selective Forwarding Unit)
**개념:** WebRTC에서 스트림을 효율적으로 분배하는 서버  
**동작 방식:**
1. 방송자의 스트림을 한 번만 수신
2. 각 시청자에게 선택적으로 전달
3. 서버 리소스 최적화

### HLS (HTTP Live Streaming)
**개념:** HTTP 기반 적응형 스트리밍 프로토콜  
**파일 구조:**
```
master.m3u8 (마스터 플레이리스트)
├── 1080p.m3u8
│   ├── segment0.ts
│   ├── segment1.ts
│   └── ...
├── 720p.m3u8
│   ├── segment0.ts
│   ├── segment1.ts
│   └── ...
└── 480p.m3u8
    ├── segment0.ts
    ├── segment1.ts
    └── ...
```

### Nginx-RTMP
**개념:** RTMP 프로토콜 지원 Nginx 모듈  
**설정 예시:**
```nginx
rtmp {
    server {
        listen 1935;
        
        application live {
            live on;
            record off;
            
            # RTMP를 HLS로 자동 변환
            hls on;
            hls_path /tmp/hls;
            hls_fragment 3;
            hls_playlist_length 60;
        }
    }
}
```

---

## 🚀 구현 시작하기

1. **프로토타입 단계**: Progressive Download로 VOD 구현
2. **확장 단계**: HLS 적응형 스트리밍 추가
3. **실시간 단계**: RTMP + HLS 실시간 방송 구현
4. **최적화 단계**: CDN, 캐싱, 분산 시스템 적용

이 가이드를 참고하여 점진적으로 기능을 구현하며 스트리밍 시스템의 복잡성을 이해해보세요!