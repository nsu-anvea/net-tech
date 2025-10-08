import socket
import os

if __name__ == "__main__":
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind(("127.0.0.1", 2000))
    server.listen(4)

    print("Server is running on http://127.0.0.1:2000")

    try:
        while True:
            client_socket, address = server.accept()
            data = client_socket.recv(1024).decode("utf-8")
            print(f"Received request from {address}:\n{data}")

            # Пытаемся получить путь из первой строки запроса
            try:
                request_line = data.splitlines()[0]
                method, path, _ = request_line.split()
                file_path = path.lstrip("/")
            except ValueError:
                client_socket.close()
                continue

            # Попытка прочитать файл и отправить содержимое
            if os.path.exists(file_path) and os.path.isfile(file_path):
                with open(file_path, "rb") as f:
                    content = f.read()
                headers = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n\r\n"
                client_socket.send(headers.encode("utf-8") + content)
            else:
                headers = "HTTP/1.1 404 Not Found\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n"
                client_socket.send(headers.encode("utf-8") + b"404 Not Found")

            client_socket.close()

    except KeyboardInterrupt:
        print("\nShutting down server...")
    finally:
        server.close()
        print("Server was turned off.")