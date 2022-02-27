package pack;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

import static java.nio.channels.SelectionKey.*;
import static java.nio.charset.StandardCharsets.*;

public class ClientTcp {
    public static void main(String[] args) {
        new ClientTcp().start();
    }

    final String ADR = "localhost";
    final int PORT = 3030;

    final void start() {
        System.out.println("[CLIENT]");
        InetSocketAddress address = new InetSocketAddress(ADR, PORT);
        try (SocketChannel socketChannel = SocketChannel.open(address)) {
            Selector selector = Selector.open();
            socketChannel.configureBlocking(false);
            ByteBuffer buf = ByteBuffer.allocate(256);
            socketChannel.register(selector, OP_READ);
            new Thread(() -> {
                try {
                    while (true) {
                        selector.select();
                        Set<SelectionKey> set = selector.selectedKeys();
                        Iterator<SelectionKey> keys = set.iterator();
                        while (keys.hasNext()) {
                            SelectionKey key = keys.next();
                            keys.remove();
                            if (key.isReadable()) {
                                SocketChannel client = (SocketChannel) key.channel();
                                buf.clear();
                                client.read(buf);
                                buf.flip();
                                String str = new String(buf.array(), buf.position(), buf.limit());
                                System.out.println(str);
                            }
                        }
                        set.clear();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            readBuffer(socketChannel, buf);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readBuffer(SocketChannel socketChannel, ByteBuffer buf) {
        Scanner sc = new Scanner(System.in);
        String line;

        while (true) {
            line = sc.nextLine();
            buf.clear();
            buf.put(line.getBytes(UTF_8));
            buf.flip();
            try {
                socketChannel.write(buf);
                if (line.equalsIgnoreCase("/q")) {
                    System.out.println("Client closed");
                    System.exit(0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
