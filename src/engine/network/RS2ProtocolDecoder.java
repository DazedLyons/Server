package engine.network;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import engine.util.ISAACRandomGen;
import game.entity.player.Player;
import game.net.packets.Packet;

/*
 * Project Insanity - Evolved v.3
 * RS2ProtocolDecoder,java
 */

public class RS2ProtocolDecoder extends CumulativeProtocolDecoder {

	private final ISAACRandomGen isaac;

	/**
	 * To make sure only the CodecFactory can initialise us.
	 */
	protected RS2ProtocolDecoder(final ISAACRandomGen isaac) {
		this.isaac = isaac;
	}

	@Override
	/**
	 * Releases resources used by this decoder.
	 * @param session
	 */
	public void dispose(final IoSession session) throws Exception {
		super.dispose(session);
	}

	/**
	 * Decodes a message.
	 * 
	 * @param session
	 * @param in
	 * @param out
	 * @return
	 */
	@Override
	protected boolean doDecode(final IoSession session, final ByteBuffer in,
			final ProtocolDecoderOutput out) throws Exception {
		synchronized (session) { // TODO CHANGE!!!!!!!
			/*
			 * Fetch the ISAAC cipher for this session.
			 */
			// ISAACRandomGen inCipher = ((Player)
			// session.getAttribute("player")).getInStreamDecryption();

			/*
			 * Fetch any cached opcodes and sizes, reset to -1 if not present.
			 */
			int opcode = (Integer) session.getAttribute("opcode");
			int size = (Integer) session.getAttribute("size");

			/*
			 * If the opcode is not present.
			 */
			if (opcode == -1) {
				/*
				 * Check if it can be read.
				 */
				if (in.remaining() >= 1) {
					/*
					 * Read and decrypt the opcode.
					 */
					opcode = in.get() & 0xFF;
					opcode = opcode - isaac.getNextKey() & 0xFF;

					/*
					 * Find the packet size.
					 */
					size = Player.PACKET_SIZES[opcode];

					/*
					 * Set the cached opcode and size.
					 */
					session.setAttribute("opcode", opcode);
					session.setAttribute("size", size);
				} else {
					/*
					 * We need to wait for more data.
					 */
					return false;
				}
			}

			/*
			 * If the packet is variable-length.
			 */
			if (size == -1) {
				/*
				 * Check if the size can be read.
				 */
				if (in.remaining() >= 1) {
					/*
					 * Read the packet size and cache it.
					 */
					size = in.get() & 0xFF;
					session.setAttribute("size", size);
				} else {
					/*
					 * We need to wait for more data.
					 */
					return false;
				}
			}

			/*
			 * If the packet payload (data) can be read.
			 */
			if (in.remaining() >= size) {
				/*
				 * Read it.
				 */
				final byte[] data = new byte[size];
				in.get(data);
				final ByteBuffer payload = ByteBuffer.allocate(data.length);
				payload.put(data);
				payload.flip();

				/*
				 * Produce and write the packet object.
				 */
				out.write(new Packet(session, opcode, data));

				/*
				 * Reset the cached opcode and sizes.
				 */
				session.setAttribute("opcode", -1);
				session.setAttribute("size", -1);

				/*
				 * Indicate we are ready to read another packet.
				 */
				return true;
			}

			/*
			 * We need to wait for more data.
			 */
			return false;
		}
	}

}