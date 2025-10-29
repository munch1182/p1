import asyncio
import json
import logging
from typing import Dict, Set
from websockets import serve, WebSocketServerProtocol

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("SignalingServer")

# requirements.txt
# websockets>=11.0.0

class SignalingServer:
    def __init__(self):
        self.rooms: Dict[str, Set[WebSocketServerProtocol]] = {}

    async def register(self, websocket: WebSocketServerProtocol, room_id: str):
        """注册用户到房间"""
        if room_id not in self.rooms:
            self.rooms[room_id] = set()

        # 检查房间是否已满（限制2人）
        if len(self.rooms[room_id]) >= 2:
            await websocket.close(1000, "房间已满")
            return

        self.rooms[room_id].add(websocket)
        logger.info(f"用户加入房间 {room_id}, 当前人数: {len(self.rooms[room_id])}")

        # 通知房间内其他用户有新用户加入
        if len(self.rooms[room_id]) > 1:
            join_message = json.dumps({"type": "user-joined"})
            await self.send_to_others(websocket, room_id, join_message)

    async def unregister(self, websocket: WebSocketServerProtocol, room_id: str):
        """从房间移除用户"""
        if room_id in self.rooms:
            self.rooms[room_id].discard(websocket)
            logger.info(f"用户离开房间 {room_id}, 剩余人数: {len(self.rooms[room_id])}")

            if not self.rooms[room_id]:
                del self.rooms[room_id]

    async def send_to_others(
        self, sender: WebSocketServerProtocol, room_id: str, message: str
    ):
        """向房间内其他用户发送消息"""
        if room_id in self.rooms:
            for websocket in self.rooms[room_id]:
                if websocket != sender:
                    try:
                        await websocket.send(message)
                        logger.info(f"向其他用户发送消息: {message}")
                    except Exception as e:
                        logger.error(f"发送消息失败: {e}")

    async def handle_message(
        self, websocket: WebSocketServerProtocol, room_id: str, message: str
    ):
        """处理来自客户端的消息"""
        try:
            data = json.loads(message)
            message_type = data.get("type")

            logger.info(f"收到消息类型: {message_type}，房间: {room_id}")

            # 将消息转发给房间内的其他用户
            await self.send_to_others(websocket, room_id, message)

        except json.JSONDecodeError as e:
            logger.error(f"JSON解析错误: {e}")
        except Exception as e:
            logger.error(f"处理消息错误: {e}")

    async def handler(self, websocket: WebSocketServerProtocol):
        """WebSocket连接处理器"""
        room_id = "default"
        try:
            # 从查询参数获取房间ID
            path = websocket.request.path

            if "?" in path:
                params = path.split("?")[1]
                for param in params.split("&"):
                    if param.startswith("room="):
                        room_id = param.split("=")[1]
                        break

            logger.info(f"新连接加入房间: {room_id}")

            # 注册用户
            await self.register(websocket, room_id)

            # 处理消息
            async for message in websocket:
                await self.handle_message(websocket, room_id, message)

        except Exception as e:
            logger.error(f"连接处理错误: {e}")
        finally:
            # 断开连接时清理
            await self.unregister(websocket, room_id)

    async def start_server(self, host: str = "0.0.0.0", port: int = 8888):
        """启动信令服务器"""
        logger.info(f"启动信令服务器在 {host}:{port}")
        async with serve(self.handler, host, port):
            await asyncio.Future()  # 永久运行


def main():
    server = SignalingServer()

    # 启动服务器
    asyncio.run(server.start_server())


if __name__ == "__main__":
    main()
