package jol.net.udp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import jol.net.Address;
import jol.net.Channel;
import jol.net.IP;
import jol.net.Message;
import jol.net.Server;
import jol.types.basic.Tuple;
import jol.types.basic.BasicTupleSet;
import jol.types.exception.JolRuntimeException;
import jol.types.exception.UpdateException;
import jol.types.table.TableName;
import jol.core.Runtime;

public class UDP extends Server {
	private static final TableName UDPMessage = new TableName("udp", "message");

	public static final int max_packet  = 1500;
	public static final int max_payload = 1400;

	private Runtime context;

	private DatagramSocket server;

	public UDP(Runtime context, Integer port) throws IOException, UpdateException, JolRuntimeException {
		super("UDP Server");
		this.context = context;
		this.server = new DatagramSocket(port);

        // NB: we need to evaluate() here to avoid a race condition: the
        // UDP program must be registered before we accept any packets
		context.install("system", "jol/net/udp/udp.olg");
		context.evaluate();
	}

	public int getLocalPort() {
		return this.server.getLocalPort();
	}

	/**
	 * Receives UDP packets and adds them to the UDP Buffer table.
	 */
	@Override
	public void run() {
		while (true) {
			try {
				byte[] buf = new byte[max_packet];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				server.receive(packet);

				try {
					ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(buf));
					Message message = (Message) input.readObject();
					Tuple tuple = new Tuple("receive", new IP(packet.getAddress(), packet.getPort()), message);
					context.schedule("udp", UDPMessage, new BasicTupleSet(tuple), null);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public Channel open(Address address) {
		try {
			return new Connection(address);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void close(Channel channel) {
		((Connection) channel).close();
	}

    @Override
    public void cleanup() {
        // Nothing to do for UDP cleanup
    }

	private static class Connection extends Channel {
		private DatagramSocket socket;

		private IP address;

		public Connection(Address address) throws SocketException {
			super("udp", address);
			this.socket = new DatagramSocket();
			this.address = (IP) address;
		}

		@Override
		public Address address() {
			return this.address;
		}

		@Override
		public boolean send(Message message) {
			try {
				ByteArrayOutputStream data = new ByteArrayOutputStream(UDP.max_packet);
				ObjectOutputStream oss = new ObjectOutputStream(data);
				oss.writeObject(message);
				if (data.size() > UDP.max_packet) {
					System.err.println("UDP packet exceeds maximum allowable size!");
					return false;
				}

				DatagramPacket dgram = new DatagramPacket(data.toByteArray(), data.size());
				this.socket.send(dgram);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}

		private void close() {
			socket.close();
		}
	}
}
