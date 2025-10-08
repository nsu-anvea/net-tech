import socket
import os
import mimetypes

def get_content_type(path):
    mime_type, _ = mimetypes.guess_type(path)
    return mime_type or "application/octet-stream"

def build_response(status_code, content, content_type="text/plain"):
    reason = {
        200: "OK",
        404: "Not Found",
        400: "Bad Request",
        500: "Internal Server Error"
    }.get(status_code, "OK")

    headers = f"HTTP/1.1 {status_code} {reason}\r\n"
    headers += f"Content-Type: {content_type}; charset=utf-8\r\n"
    headers += f"Content-Length: {len(content)}\r\n"
    headers += "Connection: close\r\n\r\n"

    return headers.encode("utf-8") + content

def handle_get(path):
    file_path = path.lstrip("/")
    if os.path.exists(path) and os.path.isfile(file_path):
        with open(file_path, "rb") as f:
            content = f.read()
        content_type = get_content_type(file_path)
        return build_response(200, content, content_type)
    else:
        return build_response(404, b"404 Not Found", "text/plain")

def handle_post(path, body):
    file_path = path.lstrip("/")
    try:
        with open(file_path, "wb") as f:
            f.write(body)
        return build_response(200, b"Data saved", "text/plain")
    except Exception as e:
        return build_response(500, str(e).encode(), "text/plain")


if __name__ == "__main__":
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind(("127.0.0.1", 2000))
    server.listen(4)

    print("Server is running on http://127.0.0.1:2000")

    try:
        while True:
            client_socket, address = server.accept()
            data = b""
            while True:
                part = client_socket.recv(1024)
                data += part
                if b"\r\n\r\n" in data:
                    break

            request = data.decode("utf-8", errors="ignore")
            print(f"Received request:\n{request}")

            try:
                lines = request.split("\r\n")
                request_line = lines[0]
                method, path, _ = request_line.split()

                if method == "GET":
                    response = handle_get(path)

                elif method == "POST":
                    # Получаем длину тела
                    headers = {}
                    for line in lines[1:]:
                        if ": " in line:
                            key, value = line.split(": ", 1)
                            headers[key.lower()] = value

                    content_length = int(headers.get("content-length", 0))
                    body_start = data.find(b"\r\n\r\n") + 4
                    body = data[body_start:]

                    # Если тело пришло не полностью — ждем остаток
                    while len(body) < content_length:
                        body += client_socket.recv(1024)

                    response = handle_post(path, body)

                else:
                    response = build_response(400, b"Unsupported method", "text/plain")

            except Exception as e:
                print("Error:", e)
                response = build_response(500, b"Internal Server Error", "text/plain")

            client_socket.sendall(response)
            client_socket.close()

    except KeyboardInterrupt:
        print("\nShutting down server...")
    finally:
        server.close()
        print("Server was turned off.")