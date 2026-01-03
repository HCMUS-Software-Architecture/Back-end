# Building price candle real time with websocket and STOMP

Tuyệt vời! Dưới đây là tài liệu kỹ thuật (Technical Documentation) mẫu mực mà bạn có thể gửi ngay cho team Frontend (FE). Tài liệu này được viết theo chuẩn tích hợp hệ thống, rõ ràng, tách bạch giữa API và Logic xử lý.

---

# 📄 TÀI LIỆU TÍCH HỢP REAL-TIME TRADING CHART

**Version:** 1.0
**Status:** Ready for Dev
**Backend Stack:** Spring Boot + STOMP WebSocket
**Frontend Stack:** Next.js + Lightweight Charts (Recommended)

---

## 1. Tổng quan kiến trúc

Hệ thống sử dụng cơ chế **Hybrid Data Loading** để đảm bảo tốc độ và hiệu năng:

1. **Lịch sử (Historical Data):** Lấy qua **REST API** (khi user mới vào trang hoặc đổi khung thời gian).
2. **Thời gian thực (Real-time):** Lắng nghe qua **WebSocket (STOMP)** để cập nhật cây nến cuối cùng hoặc vẽ cây nến mới.

---

## 2. Thông tin kết nối (Connection Details)

### A. Thư viện yêu cầu

Frontend cần cài đặt các thư viện sau để giao tiếp với giao thức STOMP:

```bash
npm install @stomp/stompjs sockjs-client

```

### B. Cấu hình WebSocket

* **Endpoint (Handshake):** `http://localhost:8081/ws/prices`
* **Protocol:** `ws`
* **Type:** SockJS + STOMP

---

## 3. Quy ước Dữ liệu (Data Contract)

### A. REST API (Lấy lịch sử) (Tham khảo swagger)



* **Response:** Mảng JSON trả về danh sách nến đã đóng (Closed Candles).

### B. WebSocket Topic (Nhận dữ liệu Real-time)

Frontend cần Subscribe vào topic theo định dạng sau:

`/topic/candles/{interval}/{symbol}`

* **{interval}:** `1m`, `5m`, `10m`, `15m`, `60m` (1h), `240m` (4h).
* **{symbol}:** `btcusdt` (Lưu ý: Viết thường).

**Ví dụ:**

* BTCUSDT khung 1 phút: `/topic/candles/1m/btcusdt`
* ETHUSDT khung 15 phút: `/topic/candles/15m/ethusdt`

### C. Cấu trúc Object Nến (DTO)

Dữ liệu từ REST API và WebSocket đều trả về chung một cấu trúc:

```typescript
interface CandleDTO {
    symbol: string;      // VD: "BTCUSDT"
    openTime: number;    // Unix Timestamp (Milliseconds) - VD: 1709283000000
    open: number;        // Giá mở cửa
    high: number;        // Giá cao nhất
    low: number;         // Giá thấp nhất
    close: number;       // Giá hiện tại (hoặc đóng cửa)
    volume: number;      // Khối lượng giao dịch
}

```

---

## 4. Giải thuật xử lý tại Frontend (Quan trọng)

Vì WebSocket bắn tick liên tục (500ms/lần), Frontend cần xử lý logic **Merge (Gộp)** để biểu đồ không bị giật.

### Quy trình chuẩn:

1. **Bước 1: Init Chart** -> Gọi REST API lấy 500 nến quá khứ -> Vẽ lên chart.
2. **Bước 2: Connect WebSocket** -> Subscribe vào đúng topic của interval hiện tại.
3. **Bước 3: Xử lý Message (On Message Received):**

Frontend cần so sánh `openTime` của gói tin vừa nhận được với `time` của cây nến cuối cùng đang hiển thị trên biểu đồ:

* **Trường hợp 1: Update (Cập nhật)**
* *Điều kiện:* `message.openTime === lastCandle.time`
* *Hành động:* Dùng hàm `update()` của chart để sửa lại giá `High`, `Low`, `Close`, `Volume` của cây nến hiện tại.


* **Trường hợp 2: New Candle (Tạo mới)**
* *Điều kiện:* `message.openTime > lastCandle.time`
* *Hành động:* Chart tự động "đóng băng" nến cũ. Thêm một cây nến mới tinh vào bên phải với dữ liệu từ message.



---

## 5. Code mẫu tích hợp (React/Next.js + Lightweight Charts)

Dưới đây là component mẫu đã xử lý đầy đủ logic reconnect và switch khung thời gian.

```tsx
import React, { useEffect, useRef, useState } from 'react';
import { createChart, IChartApi, ISeriesApi } from 'lightweight-charts';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

interface TradingChartProps {
    symbol: string;   // VD: 'BTCUSDT'
    interval: string; // VD: '1m', '5m', '15m'
}

const TradingChart: React.FC<TradingChartProps> = ({ symbol, interval }) => {
    const chartContainerRef = useRef<HTMLDivElement>(null);
    const chartRef = useRef<IChartApi | null>(null);
    const seriesRef = useRef<ISeriesApi<"Candlestick"> | null>(null);
    const stompClientRef = useRef<Client | null>(null);

    // 1. Khởi tạo Chart
    useEffect(() => {
        if (!chartContainerRef.current) return;

        const chart = createChart(chartContainerRef.current, {
            width: chartContainerRef.current.clientWidth,
            height: 500,
            timeScale: {
                timeVisible: true,
                secondsVisible: false,
            },
        });

        const candlestickSeries = chart.addCandlestickSeries();
        chartRef.current = chart;
        seriesRef.current = candlestickSeries;

        return () => chart.remove();
    }, []);

    // 2. Fetch Data & Connect Socket khi Symbol/Interval thay đổi
    useEffect(() => {
        const fetchDataAndConnect = async () => {
            if (!seriesRef.current) return;

            // A. Gọi REST API lấy lịch sử
            try {
                const response = await fetch(`http://localhost:8080/api/candles?symbol=${symbol}&interval=${interval}&limit=1000`);
                const data = await response.json();
                
                // Map dữ liệu về chuẩn của Lightweight Charts (time là seconds)
                const formattedData = data.map((item: any) => ({
                    time: item.openTime / 1000, // Backend gửi ms, Chart cần seconds
                    open: item.open,
                    high: item.high,
                    low: item.low,
                    close: item.close,
                }));

                // Reset dữ liệu cũ và set dữ liệu mới
                seriesRef.current.setData(formattedData);
                
                // B. Sau khi có data nền, mới bắt đầu nối Socket
                connectWebSocket();
            } catch (error) {
                console.error("Lỗi tải lịch sử:", error);
            }
        };

        fetchDataAndConnect();

        // Cleanup: Ngắt kết nối khi đổi symbol/interval để tránh leak memory
        return () => disconnectWebSocket();
    }, [symbol, interval]);

    const connectWebSocket = () => {
        // Ngắt kết nối cũ nếu có
        disconnectWebSocket();

        const socket = new SockJS('http://localhost:8080/ws/prices');
        const client = new Client({
            webSocketFactory: () => socket,
            reconnectDelay: 5000, // Tự động reconnect sau 5s nếu mất mạng
            onConnect: () => {
                console.log(`Connected to ${symbol} - ${interval}`);
                
                // Subscribe đúng Topic
                // Lưu ý: interval phải map đúng với backend (1m, 5m...)
                const topic = `/topic/candles/${interval}/${symbol.toLowerCase()}`;
                
                client.subscribe(topic, (message) => {
                    const candleDto = JSON.parse(message.body);
                    
                    // Logic Real-time Update
                    if (seriesRef.current) {
                        seriesRef.current.update({
                            time: candleDto.openTime / 1000, // Convert sang seconds
                            open: candleDto.open,
                            high: candleDto.high,
                            low: candleDto.low,
                            close: candleDto.close,
                        });
                    }
                });
            },
        });

        client.activate();
        stompClientRef.current = client;
    };

    const disconnectWebSocket = () => {
        if (stompClientRef.current) {
            stompClientRef.current.deactivate();
        }
    };

    return <div ref={chartContainerRef} style={{ position: 'relative' }} />;
};

export default TradingChart;

```

---

## 6. Các lưu ý quan trọng cho FE (Notes)

1. **Timezone:** Dữ liệu `openTime` trả về là **Unix Timestamp (UTC)**. Các thư viện chart thường tự xử lý việc convert sang giờ địa phương của trình duyệt người dùng. Không cần cộng trừ giờ thủ công.
2. **Đơn vị thời gian:** Java backend trả về **Milliseconds** (13 chữ số). Lightweight Charts dùng **Seconds** (10 chữ số). **Nhớ chia cho 1000** trước khi đưa vào chart.
3. **Tần suất Update:** Socket sẽ bắn data khoảng 500ms/lần. Đây là tần suất tối ưu, không nên debounce hay throttle thêm ở phía FE để tránh bị trễ giá (lag).
4. **Xử lý UI:** Khi User chuyển khung thời gian (ví dụ bấm nút "15m"), cần **clear** toàn bộ dữ liệu chart cũ (`series.setData([])`) trước khi load API mới để tránh nến cũ và mới bị vẽ đè lên nhau.