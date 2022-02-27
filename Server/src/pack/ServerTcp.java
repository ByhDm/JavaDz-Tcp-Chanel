package pack;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

import static java.nio.channels.SelectionKey.*;
import static java.nio.charset.StandardCharsets.*;

public class ServerTcp {
    public static void main(String[] args) {
        new ServerTcp().start();
    }

    final String ADR = "localhost";
    final int PORT = 3030;
    private Selector selector;

    final void start() {
        try (ServerSocketChannel socket = ServerSocketChannel.open()) {
            selector = Selector.open();
            InetSocketAddress socketAddress = new InetSocketAddress(ADR, PORT);
            socket.bind(socketAddress);
            socket.configureBlocking(false);
            socket.register(selector, OP_ACCEPT);
            System.out.println("[SERVER] started...");
            while (true) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = keys.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();
                    if (key.isAcceptable()) {
                        SocketChannel channel = socket.accept();
                        channel.configureBlocking(false);
                        System.out.println("server> connected new client: "
                                + channel.getRemoteAddress());
                        channel.register(selector, OP_READ);
                        key.interestOps(OP_ACCEPT);
                    } else if (key.isReadable()) {
                        SocketChannel channel = (SocketChannel) key.channel();
                        ByteBuffer buf = ByteBuffer.allocate(256);
                        channel.read(buf);
                        String line = new String(buf.array()).trim();
                        System.out.println("from client["
                                + channel.getRemoteAddress()
                                + "] message> " + line);
                        if (line.equalsIgnoreCase("/q")) {
                            broadcast("server> from client["
                                    + channel.getRemoteAddress()
                                    + "]> closed");
                            channel.close();
                        } else {
                            broadcast("server> from client["
                                    + channel.getRemoteAddress()
                                    + "]> "
                                    + line);
                        }
                    }
                }
            }
        } catch (
                Exception e) {
            e.printStackTrace();
        }
    }

    private void broadcast(String message) {
        byte[] byteArray = message.getBytes(UTF_8);
        ByteBuffer buf = ByteBuffer.wrap(byteArray);
        for (SelectionKey key : selector.keys()) {
            Channel targetChannel = key.channel();
            if (targetChannel.isOpen() && targetChannel instanceof  SocketChannel) {
                SocketChannel channel = (SocketChannel) targetChannel;
                try {
                    channel.write(buf);
                    buf.flip();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

