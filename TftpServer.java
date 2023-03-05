//Shifaz Ali, 1323080
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;

class TftpServerWorker extends Thread
{
    private DatagramPacket req;

    public void run()
    {
        DatagramSocket ds;
        try {
            /* parse the request packet, ensuring that it is an RRQ.*/
            TftpPacket.Type reqType = TftpPacket.parse(req).getType();

            /*
             * make a note of the address and port the client's request
             * came from
             */
            InetAddress clientAddress = req.getAddress();
            int clientPort = req.getPort();

            /* create a datagram socket to send on, setSoTimeout to 1s (1000ms) */
            ds = new DatagramSocket();
            ds.setSoTimeout(1000);

            String filename = TftpPacket.parse(req).getFilename();
            if(reqType.equals(TftpPacket.Type.RRQ)){

                /* try to open the file.  if not found, send an error */
                File file = new File(filename);
                if(!file.exists()){
                    DatagramPacket error = TftpPacket.createERROR(clientAddress, clientPort, filename);
                    ds.send(error);
                }
            }

            /*
             * Allocate a txbuf byte buffer 512 bytes in size to read
             * chunks of a file into, and declare an integer that keeps
             * track of the current block number initialized to one.
             *
             * allocate a rxbuf byte buffer 2 bytes in size to receive
             * TFTP ack packets into, and then allocate a DatagramPacket
             * backed by that rxbuf to pass to the DatagramSocket::receive
             * method
             */
            byte[] txBuf = new byte[512];
            int currentBlock = 1;
            byte[] rxBuf = new byte[2];
            DatagramPacket packetAck = new DatagramPacket(rxBuf, rxBuf.length);
            ds.receive(packetAck);

            FileInputStream fis;
            while(true) {
                /*
                 * read a chunk from the file, and make a note of the size
                 * read.  if we get EOF, signalled by
                 * FileInputStream::read returning -1, then set the chunk
                 * size to zero to cause an empty block to be sent.
                 */
                fis = new FileInputStream(filename);
                int size = fis.read(txBuf);

                if(size == -1){
                    DatagramPacket emptyPacket = new DatagramPacket(new byte[0], 0);
                    ds.send(emptyPacket);
                }

                /*
                 * use TftpPacket.createData to create a DATA packet
                 * addressed to the client's address and port, specifying
                 * the block number, the contents of the block, and the
                 * size of the block
                 */
                DatagramPacket dataPack = TftpPacket.createDATA(clientAddress, clientPort, currentBlock, txBuf, size);

                /*
                 * declare a boolean value to control transmission through
                 * each loop, and an integer to count the number of
                 * transmission attempts made with the current block.
                 */
                boolean control = true;
                int attempts = 0;

                while(control) {

                    /*
                     * if we are to transmit the packet this pass through
                     * the loop, send the packet and increment the number
                     * of attempts we have made with this block.  set the
                     * boolean value to false to prevent the packet being
                     * retransmitted except on a SocketTimeoutException,
                     * noted below.
                     */
                    ds.send(dataPack);
                    attempts++;
                    control = false;

                    /*
                     * call receive, looking for an ACK for the current
                     * block number.  if we get an ack, break out of the
                     * retransmission loop.  otherwise, if we get a
                     * SocketTimeoutException, set the boolean value to
                     * true.  if we have tried five times, then we break
                     * out of the loop to give up.
                     */
                   try{
                       ds.receive(packetAck);
                       if(currentBlock == TftpPacket.parse(packetAck).getBlock()){
                           if(TftpPacket.Type.ACK.equals(TftpPacket.parse(packetAck).getType())) {
                                break;
                           }
                       }
                       if(attempts == 5){
                           break;
                       }
                   }catch (SocketTimeoutException s){
                       System.err.println("Error: " + s);
                       control = true;
                   }

                }

                /*
                 * outside of the loop, determine if we just sent our last
                 * transmission (the block size was less than 512 bytes,
                 * or we tried five times without getting an ack
                 */
                if(size < 512 && size > 0 || attempts == 5){
                    break;
                }

                /*
                 * use TftpPacket.nextBlock to determine the next block
                 * number to use.
                 */
                currentBlock = TftpPacket.nextBlock(currentBlock);
            }

            /* cleanup: close the FileInputStream and the DatagramSocket */
            fis.close();
            ds.close();
            return;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TftpServerWorker(DatagramPacket req)
    {
        this.req = req;
    }
}

class TftpServer
{
    public static void main(String[] args)
    {
        try {
            /*
             * allocate a DatagramSocket, and find out what port it is
             * listening on
             */
            DatagramSocket ds = new DatagramSocket();
            System.out.println("TftpServer on port " + ds.getLocalPort());

            byte[] buf = new byte[1472];
            for(int i = 0; i < buf.length; i ++) {
                /*
                 * allocate a byte buffer to back a DatagramPacket
                 * with.  I suggest 1472 byte array for this.
                 * allocate the corresponding DatagramPacket, and call
                 * DatagramSocket::receive
                 */
                DatagramPacket p = new DatagramPacket(buf, 1472);
                ds.receive(p);

                /*
                 * allocate a new worker thread to process this
                 * packet.  implement the logic looking for a RRQ in
                 * the worker thread's run method.
                 */
                TftpServerWorker worker = new TftpServerWorker(p);
                worker.start();
            }
        }
        catch(Exception e) {
            System.err.println("TftpServer::main Exception: " + e);
        }

        return;
    }
}
