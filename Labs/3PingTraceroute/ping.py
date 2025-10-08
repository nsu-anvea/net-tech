import os
import socket
import struct
import time
import select
import sys


def checksum(data):
    sum = 0
    for i in range(0, len(data), 2):
        if i + 1 < len(data):
            word = (data[i] << 8) + data[i + 1]
            sum += word
        else:
            sum += data[i] << 8

    sum = (sum >> 16) + (sum & 0xffff)
    sum += sum >> 16
    return ~sum & 0xffff


def create_icmp_packet(identifier, sequence):

    header = struct.pack('!BBHHH', 8, 0, 0, identifier, sequence)

    data = struct.pack('!d', time.time())

    chksum = checksum(header + data)

    header = struct.pack('!BBHHH', 8, 0, chksum, identifier, sequence)

    return header + data


def ping(dest_addr, timeout=1, count=4):
    try:
        dest_ip = socket.gethostbyname(dest_addr)
    except socket.gaierror:
        print(f"Не удалось разрешить имя {dest_addr}")
        return

    try:
        my_socket = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_ICMP)
        my_socket.settimeout(timeout)
    except PermissionError:
        print("Для работы требуется права администратора/root")
        return

    pid = os.getpid() & 0xffff
    seq_number = 1
    packets_sent = 0
    packets_received = 0
    rtts = []

    print(f"PING {dest_addr} ({dest_ip})")

    try:
        for i in range(count):
            packet = create_icmp_packet(pid, seq_number + i)
            send_time = time.time()
            my_socket.sendto(packet, (dest_ip, 0))
            packets_sent += 1

            # В ready оказываются сокеты, готовые для чтения
            ready = select.select([my_socket], [], [], timeout)
            if ready[0]:
                recv_packet, addr = my_socket.recvfrom(1024)
                recv_time = time.time()

                icmp_header = recv_packet[20:28] # первые 20 байт IP-заголовок
                type, code, chksum, p_id, sequence = struct.unpack('!BBHHH', icmp_header) # сетевой порядок байт: B - 1 байт
                                                                                                                       # H - 2 байта
                if p_id == pid:
                    rtt = (recv_time - send_time) * 1000
                    rtts.append(rtt)
                    packets_received += 1
                    print(f"{len(recv_packet)} байт от {addr[0]}: "
                          f"icmp_seq={sequence} ttl={recv_packet[8]} time={rtt:.2f} мс")
                else:
                    print("Получен ответ на другой запрос")
            else:
                print(f"Таймаут запроса для icmp_seq={seq_number + i}")

            time.sleep(1)

        if packets_received > 0:
            avg_rtt = sum(rtts) / len(rtts)
            min_rtt = min(rtts)
            max_rtt = max(rtts)
            loss = (packets_sent - packets_received) / packets_sent * 100
            print(f"\n--- {dest_addr} статистика ping ---")
            print(f"{packets_sent} пакетов передано, {packets_received} получено, {loss:.1f}% потерь")
            print(f"rtt min/avg/max = {min_rtt:.2f}/{avg_rtt:.2f}/{max_rtt:.2f} мс")
        else:
            print(f"\n{packets_sent} пакетов передано, 0 получено, 100% потерь")

    except KeyboardInterrupt:
        print("\nPing прерван пользователем")
    finally:
        my_socket.close()


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Too few arguments.")
        print("Using: sudo python ping.py <host>")
        sys.exit(1)

    target = sys.argv[1]
    ping(target)