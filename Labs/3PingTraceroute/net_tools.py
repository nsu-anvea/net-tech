import os
import socket
import struct
import time
import select
import sys
import matplotlib.pyplot as plt
import networkx as nx
from collections import defaultdict


class ICMPPacket:
    """Класс для работы с ICMP-пакетами"""

    def __init__(self, type=8, code=0, id=None, seq=1, data=b''):
        self.type = type
        self.code = code
        self.id = id or (os.getpid() & 0xFFFF)
        self.seq = seq
        self.data = data or struct.pack('!d', time.time())
        self.checksum = 0

    def pack(self):
        """Упаковка пакета в бинарный формат"""
        header = struct.pack('!BBHHH', self.type, self.code, 0, self.id, self.seq)
        self.checksum = self.calculate_checksum(header + self.data)
        return struct.pack('!BBHHH', self.type, self.code, self.checksum, self.id, self.seq) + self.data

    @staticmethod
    def calculate_checksum(data):
        """Вычисление контрольной суммы"""
        sum = 0
        for i in range(0, len(data), 2):
            word = (data[i] << 8) + (data[i + 1] if i + 1 < len(data) else 0)
            sum += word
        sum = (sum >> 16) + (sum & 0xFFFF)
        return ~sum & 0xFFFF

    @classmethod
    def unpack(cls, data):
        """Распаковка ICMP-пакета"""
        header = data[:8]
        type, code, checksum, id, seq = struct.unpack('!BBHHH', header)
        return cls(type, code, id, seq, data[8:])


class NetworkTools:
    def __init__(self):
        self.ipv6 = False
        self.timeout = 1
        self.max_hops = 30
        self.port = 33434
        self.ttl = 1
        self.pid = os.getpid() & 0xFFFF
        self.graph = nx.Graph()
        self.route_info = defaultdict(dict)

    def create_socket(self, proto, is_ipv6=False):
        """Создание сокета с учетом IPv6"""
        family = socket.AF_INET6 if is_ipv6 else socket.AF_INET
        sock_type = socket.SOCK_RAW if proto == socket.IPPROTO_ICMP else socket.SOCK_DGRAM
        try:
            sock = socket.socket(family, sock_type, proto)
            sock.settimeout(self.timeout)
            return sock
        except PermissionError:
            print("Требуются права администратора!")
            sys.exit(1)

    def ping(self, host, count=4, ttl=None, ipv6=False):
        """Улучшенная реализация ping с поддержкой IPv6 и TTL"""
        try:
            dest_addr = socket.getaddrinfo(host, None, socket.AF_INET6 if ipv6 else socket.AF_INET)[0][4][0]
        except socket.gaierror:
            print(f"Не удалось разрешить имя {host}")
            return

        icmp_proto = socket.IPPROTO_ICMPV6 if ipv6 else socket.IPPROTO_ICMP
        with self.create_socket(icmp_proto, ipv6) as sock:
            if ttl:
                sock.setsockopt(socket.SOL_IPV6 if ipv6 else socket.SOL_IP,
                                socket.IPV6_UNICAST_HOPS if ipv6 else socket.IP_TTL,
                                ttl)

            print(f"PING {host} ({dest_addr}) {'IPv6' if ipv6 else 'IPv4'}")

            for seq in range(count):
                packet = ICMPPacket(id=self.pid, seq=seq)
                send_time = time.time()
                sock.sendto(packet.pack(), (dest_addr, 0))

                try:
                    data, addr = sock.recvfrom(1024)
                    recv_time = time.time()

                    # Для IPv6 ICMP находится после IPv6 заголовка и расширений
                    offset = 40 if ipv6 else 20
                    icmp_packet = ICMPPacket.unpack(data[offset:])

                    if icmp_packet.id == self.pid:
                        rtt = (recv_time - send_time) * 1000
                        print(f"{len(data)} байт от {addr[0]}: время={rtt:.2f} мс")
                except socket.timeout:
                    print("Таймаут запроса")

                time.sleep(1)

    def traceroute(self, host, ipv6=False):
        """Полноценный traceroute с измерением задержек и визуализацией"""
        try:
            dest_addr = socket.getaddrinfo(host, None, socket.AF_INET6 if ipv6 else socket.AF_INET)[0][4][0]
        except socket.gaierror:
            print(f"Не удалось разрешить имя {host}")
            return

        print(f"\nТрассировка маршрута к {host} [{dest_addr}]")
        print(f"с максимальным числом прыжков {self.max_hops}\n")

        icmp_proto = socket.IPPROTO_ICMPV6 if ipv6 else socket.IPPROTO_ICMP
        with self.create_socket(icmp_proto, ipv6) as recv_socket, \
                self.create_socket(socket.IPPROTO_UDP, ipv6) as send_socket:

            prev_addr = None
            while self.ttl <= self.max_hops:
                # Устанавливаем TTL для отправляемого пакета
                ttl_opt = socket.IPV6_UNICAST_HOPS if ipv6 else socket.IP_TTL
                send_socket.setsockopt(socket.SOL_IPV6 if ipv6 else socket.SOL_IP, ttl_opt, self.ttl)

                # Отправляем UDP-пакет
                send_time = time.time()
                send_socket.sendto(b'', (dest_addr, self.port))

                try:
                    data, addr = recv_socket.recvfrom(512)
                    recv_time = time.time()

                    # Определяем тип ICMP-ответа
                    offset = 40 if ipv6 else 20
                    icmp_packet = ICMPPacket.unpack(data[offset:])

                    rtt = (recv_time - send_time) * 1000
                    current_addr = addr[0]

                    # Записываем информацию о маршрутизаторе
                    self.route_info[self.ttl]['ip'] = current_addr
                    self.route_info[self.ttl]['rtt'] = rtt

                    print(f"{self.ttl}\t{current_addr}\t{rtt:.2f} мс")

                    # Добавляем узел в граф
                    self.graph.add_node(current_addr)
                    if prev_addr:
                        self.graph.add_edge(prev_addr, current_addr, weight=rtt)
                    prev_addr = current_addr

                    if current_addr == dest_addr:
                        break

                except socket.timeout:
                    print(f"{self.ttl}\t*\t*")

                self.ttl += 1
                self.port += 1

            self.visualize_route(host)

    def visualize_route(self, host):
        """Визуализация маршрута с помощью networkx"""
        if not self.graph.nodes:
            print("Нет данных для визуализации")
            return

        plt.figure(figsize=(12, 8))
        pos = nx.spring_layout(self.graph)

        # Рисуем узлы и ребра
        nx.draw_networkx_nodes(self.graph, pos, node_size=700)
        nx.draw_networkx_edges(self.graph, pos, width=2)

        # Подписи узлов с RTT (с проверкой наличия данных)
        labels = {}
        for i, node in enumerate(self.graph.nodes):
            rtt_info = self.route_info.get(i + 1, {})
            rtt_text = f"{rtt_info.get('rtt', 'N/A'):.2f} мс" if 'rtt' in rtt_info else "N/A"
            labels[node] = f"{node}\n{rtt_text}"

        nx.draw_networkx_labels(self.graph, pos, labels, font_size=10)

        plt.title(f"Маршрут к {host}")
        plt.axis('off')
        plt.tight_layout()
        plt.show()


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Использование:")
        print("  ping:  python net_tools.py ping <хост> [-6] [--ttl=N]")
        print("  trace: python net_tools.py trace <хост> [-6]")
        sys.exit(1)

    tool = NetworkTools()
    command = sys.argv[1]
    target = sys.argv[2]
    ipv6 = '-6' in sys.argv
    ttl = next((int(arg.split('=')[1]) for arg in sys.argv if arg.startswith('--ttl=')), None)

    if command == "ping":
        tool.ping(target, ipv6=ipv6, ttl=ttl)
    elif command == "trace":
        tool.traceroute(target, ipv6=ipv6)
    else:
        print("Неизвестная команда. Используйте 'ping' или 'trace'")