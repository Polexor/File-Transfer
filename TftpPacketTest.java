//Shifaz Ali, 1323080
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

class TftpPacketTest
{
    /*
     * construct an RRQ DatagramPacket using TftpPacket.createRRQ; pass
     * the DatagramPacket to TftpPacket.parse and ensure that the
     * contents returned by getType, getFilename are what was passed
     * in
     */
    @Test
    @DisplayName("test good RRQ")
    void testGoodRRQ() {
        InetAddress ia;
        try {
            ia = InetAddress.getByName("127.0.0.1");
        } catch(UnknownHostException e) {
            ia = null;
        }
        DatagramPacket dp = TftpPacket.createRRQ(ia, 69, "foo");
        assertNotNull(dp);
        TftpPacket tp = TftpPacket.parse(dp);
        assertNotNull(tp);
        assertEquals(TftpPacket.Type.RRQ, tp.getType(), "type is RRQ");
        assertEquals("foo", tp.getFilename(), "filename is foo");
    }

    /*
     * construct an ACK DatagramPacket using TftpPacket.createACK;
     * pass the DatagramPacket to TftpPacket.parse and ensure that the
     * contents returned by getType, getBlock are what was passed in.
     * pass a block number larger than 128 to ensure that you are
     * correctly storing values 128-255.
     */
    @Test
    @DisplayName("test good ACK")
    void testGoodACK() {
        InetAddress ia;
        try {
            ia = InetAddress.getByName("127.0.0.1");
        } catch(UnknownHostException e) {
            ia = null;
        }
        DatagramPacket dp = TftpPacket.createACK(ia, 69, 133);
        assertNotNull(dp);
        TftpPacket tp = TftpPacket.parse(dp);
        assertNotNull(tp);
        assertEquals(TftpPacket.Type.ACK, tp.getType());
        assertEquals(133, tp.getBlock());
    }

    /*
     * construct an ACK DatagramPacket using TftpPacket.createDATA;
     * pass the DatagramPacket to TftpPacket.parse and ensure that the
     * contents returned by getType, getData are what was passed in.
     * use assertArrayEquals to compare the arrays.  pass a block
     * number larger than 128 to ensure that you are correctly storing
     * values 128-255.
     */
    @Test
    @DisplayName("test good DATA")
    void testGoodDATA() {
        InetAddress ia;
        try {
            ia = InetAddress.getByName("127.0.0.1");
        } catch(UnknownHostException e) {
            ia = null;
        }
        byte[] data = {55, 66, 77};
        DatagramPacket dp = TftpPacket.createDATA(ia, 69,
                190, data, data.length);
        assertNotNull(dp);
        TftpPacket tp = TftpPacket.parse(dp);
        assertEquals(TftpPacket.Type.DATA, tp.getType(), "type is DATA");
        assertEquals(190, tp.getBlock(), "block is 190");
        assertArrayEquals(data, tp.getData(), "data is equivalent");
    }

    /*
     * construct an ERROR DatagramPacket using TftpPacket.createERROR;
     * pass the DatagramPacket to TftpPacket.parse and ensure that the
     * contents returned by getType, getError are what was passed in.
     */
    @Test
    @DisplayName("test good ERROR")
    void testGoodERR() {
        InetAddress ia;
        try{
            ia = InetAddress.getByName("127.0.0.1");
        }catch (UnknownHostException e){
            ia = null;
        }
        DatagramPacket dp = TftpPacket.createERROR(ia, 69, "error message");
        assertNotNull(dp);
        TftpPacket tp = TftpPacket.parse(dp);
        assertNotNull(tp);
        assertEquals(TftpPacket.Type.ERROR, tp.getType());
        assertEquals("error message", tp.getError());
    }

    /*
     * create a DatagramPacket with one byte, the first being an
     * invalid TFTP packet type (say 66).  assert TftpPacket.parse
     * returns null.
     */
    @Test
    @DisplayName("test invalid type")
    void testInvalidType() {
        byte[] data = {66};
        DatagramPacket dp = new DatagramPacket(data, data.length);
        TftpPacket tp = TftpPacket.parse(dp);
        assertNull(tp);
    }

    /*
     * ensure that TftpPacket.nextBlock returns the correct next block
     * value for values 1 .. 254 in a for loop.  ensure that nextBlock
     * returns the correct next block value for 255.
     */
    @Test
    @DisplayName("test nextBlock")
    void testNextBlock() {
        for (int i = 1; i <= 254 ; i++) {
            assertEquals(i + 1, TftpPacket.nextBlock(i));
        }
        assertEquals(1, TftpPacket.nextBlock(255));
    }

    /*
     * ensure that TftpPacket.lastBlock returns the correct last block
     * value for values 2 .. 255 in a for loop.  ensure that lastBlock
     * returns the correct next block value for 1.
     */
    @Test
    @DisplayName("test lastBlock")
    void testLastBlock() {
        for (int i = 2; i <= 255 ; i++) {
            assertEquals(i - 1, TftpPacket.lastBlock(i));
        }
        assertEquals(255, TftpPacket.lastBlock(1));
    }

    /*
     * create a DATA packet with no payload -- byte[] data = {};
     * ensure that TftpPacket.parse returns a structure, and then
     * ensure that getData returns an empty byte array (compare with
     * assertArrayEquals).
     */
    @Test
    @DisplayName("empty block")
    void testEmptyBlock() {
        InetAddress ia;
        try {
            ia = InetAddress.getByName("127.0.0.1");
        } catch(UnknownHostException e) {
            ia = null;
        }
        byte[] data = {};
        DatagramPacket dp = TftpPacket.createDATA(ia, 69,
                190, data, data.length);
        assertNotNull(dp);
        TftpPacket tp = TftpPacket.parse(dp);
        assertArrayEquals(data, tp.getData(), "empty data is equivalent");
    }
}
