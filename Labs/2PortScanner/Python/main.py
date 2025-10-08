import socket
import threading
from queue import Queue
import time
from datetime import datetime

try:
    import services  # Для Windows
except:
    pass


def get_service_name(port, protocol='tcp'):
    try:
        # Для Linux/Unix систем
        with open('/etc/services', 'r') as f:
            for line in f:
                if line.startswith('#') or not line.strip():
                    continue
                parts = line.split()
                if len(parts) >= 2 and parts[1] == f"{port}/{protocol}":
                    return parts[0]
    except:
        try:
            # Для Windows
            return socket.getservbyport(port, protocol)
        except:
            return "unknown"
    return "unknown"


def port_scan(port_queue, results, timeout=1):
    while not port_queue.empty():
        port = port_queue.get()
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                s.settimeout(timeout)
                result = s.connect_ex(('127.0.0.1', port))
                if result == 0:
                    service = get_service_name(port)
                    results.append((port, service))
        except:
            pass
        port_queue.task_done()


def async_port_scan(start_port, end_port, max_threads=100, timeout=1):
    port_queue = Queue()
    results = []

    for port in range(start_port, end_port + 1):
        port_queue.put(port)

    for _ in range(max_threads):
        t = threading.Thread(target=port_scan, args=(port_queue, results, timeout))
        t.daemon = True
        t.start()

    port_queue.join()
    return sorted(results, key=lambda x: x[0])


if __name__ == "__main__":
    print("=== Сканер локальных портов ===")
    print("Внимание: Используйте только для учебных целей на localhost!")

    start_port = int(input("Начальный порт (рекомендуется 1): "))
    end_port = int(input(f"Конечный порт (рекомендуется 65535, но для теста лучше 1024): "))
    timeout = float(input("Таймаут в секундах (рекомендуется 0.5-2): "))

    print(f"\nСканирование портов {start_port}-{end_port} на localhost...")
    start_time = time.time()

    open_ports = async_port_scan(start_port, end_port, timeout=timeout)

    duration = time.time() - start_time
    print(f"\nСканирование завершено за {duration:.2f} секунд")
    print("\nОткрытые порты:")
    print("Порт\tСлужба")
    print("----\t-------")
    for port, service in open_ports:
        print(f"{port}\t{service}")