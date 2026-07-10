package com.munch1182.feature.net

fun createWebSocketHtml(wsUrl: String) = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>WebSocket 测试页面</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html, body { height: 100%; font-family: Arial; }
        body { display: flex; flex-direction: column; padding: 20px; }
        h1 { margin-bottom: 10px; }
        #status { margin-bottom: 10px; }
        #log { flex: 1; border: 1px solid #ccc; padding: 10px; overflow-y: scroll; background: #f9f9f9; }
    </style>
</head>
<body>
    <h1>WebSocket 演示</h1>
    <p>连接状态: <span id="status">连接中...</span></p>
    <div id="log"></div>

    <script>
        let ws = null;

        function log(msg) {
            const logDiv = document.getElementById('log');
            logDiv.innerHTML += msg + '<br>';
            logDiv.scrollTop = logDiv.scrollHeight;
        }

        function connect() {
            const url = '$wsUrl';
            ws = new WebSocket(url);

            ws.onopen = function() {
                document.getElementById('status').innerHTML = '✅ 已连接';
                log('🟢 连接成功');
            };

            ws.onmessage = function(event) {
                log('📩 收到: ' + event.data);
            };

            ws.onclose = function(event) {
                document.getElementById('status').innerHTML = '❌ 已断开';
                log('🔴 连接关闭: ' + event.reason);
                // 自动重连（5秒后）
                setTimeout(connect, 5000);
            };

            ws.onerror = function(error) {
                log('⚠️ 错误: ' + error);
            };
        }

        // 页面加载后自动连接
        window.onload = connect;
    </script>
</body>
</html>
""".trimIndent()