//Shifaz Ali, 1323080
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

class TftpClient
{
    public static void main(String[] args)
    {
        /* expect three arguments */
        if(args.length != 3) {
            System.err.println("usage: TftpClient <name> <port> <file>\n");
            return;
        }

        /* process the command line arguments */
        String name = args[0];
        String filename = args[2];

        /*
         * use Integer.parseInt to get the number from the second
         * (port) argument
         */
        int port;
        port = Integer.parseInt(args[1]);

        FileOutputStream fos;

        /*
         * use InetAddress.getByName to get an IP address for the name
         * argument
         */
        InetAddress ia;
        /* allocate a DatagramSocket, and setSoTimeout to 6s (6000ms) */
        DatagramSocket ds;
        try{
            ia = InetAddress.getByName(name);
            ds = new DatagramSocket();
            ds.setSoTimeout(6000);

            /*
             * open an output file; preface the filename with "rx-" so
             * that you do not try to overwrite a file that the server is
             * about to try to send to you.
             */
            fos = new FileOutputStream(filename);

            /* ###
             * create a read request using TftpPacket.createRRQ and then
             * send the packet over the DatagramSocket.
             */
            DatagramPacket dpRRQ = TftpPacket.createRRQ(ia, port, filename);
            ds.send(dpRRQ);

            /*
             * declare an integer to keep track of the block that you
             * expect to receive next, initialized to one.  allocate a
             * byte buffer of 514 bytes (i.e., 512 block size plus two one
             * byte header fields) to receive DATA packets.  allocate a
             * DatagramPacket backed by that byte buffer to pass to
             * DatagramSocket::receive to receive packets into.
             */
            int expectedBlock = 1;
            byte[] buffer = new byte[514];
            DatagramPacket receiveData = new DatagramPacket(buffer, buffer.length);
            ds.receive(receiveData);

            /*
             * an infinite loop that we will eventually break out of, when
             * either an exception occurs, or we receive a block less than
             * 512 bytes in size.
             */
            while (true){
                try{
                    /*
                     * receive a packet on the DatagramSocket, and then
                     * parse it with TftpPacket.parse.  get the IP address
                     * and port where the packet came from.  The port will
                     * be different to the port you sent the RRQ to, and
                     * we will use these values to transmit the ACK to
                     */
                    TftpPacket tp = TftpPacket.parse(receiveData);
                    InetAddress dst = tp.getAddr();
                    int tpPort = tp.getPort();

                    /*
                     * if we could not parse the packet (parse returns
                     * null), then use "continue" to loop again without
                     * executing the remaining code in the loop.
                     */
                    if(TftpPacket.parse(receiveData) == null){
                        continue;
                    }

                    /*
                     * if the response is an ERROR packet, then print the
                     * error message and return.
                     */
                    if(tp.getType().equals(TftpPacket.Type.ERROR)){
                        System.out.println(tp.getError());
                        return;
                    }

                    /*
                     * if the packet is not a DATA packet, then use
                     * "continue" to loop again without executing the
                     * remaining code in the loop.
                     */
                    if(!tp.getType().equals(TftpPacket.Type.DATA)){
                        continue;
                    }

                    /*
                     * if the block number is exactly the block that we
                     * were expecting, then get the data (TftpPacket::getData)
                     * and then write it to disk.  then, send an ack for the
                     * block.  then, check to see if we received less than
                     * 512 bytes in that block; if we did, then we infer that
                     * the sender has finished, and break out of the while loop.
                     */
                    if(tp.getBlock() == expectedBlock){
                        byte[] data = tp.getData();
                        fos.write(data);
                        DatagramPacket ack = TftpPacket.createACK(dst, tpPort,tp.getBlock());
                        ds.send(ack);

                        if(data.length < 512){
                            break;
                        }
                    }
                    /*
                     * else, if the block number is the same as the block
                     * number we just received, send an ack without writing
                     * the block to disk, etc.  in this case, the server
                     * didn't receive the ACK we sent, and retransmitted.
                     */
                    else if(tp.getBlock() == TftpPacket.lastBlock(expectedBlock)){
                        byte[] data = tp.getData();
                        DatagramPacket ack = TftpPacket.createACK(dst, tpPort,tp.getBlock());
                        ds.send(ack);

                        if(data.length < 512){
                            break;
                        }
                    }
                    expectedBlock = TftpPacket.nextBlock(expectedBlock);
                }catch (Exception e){
                    System.err.println("Error: " + e);
                    break;
                }
            }
            /* cleanup -- close the output file and the DatagramSocket */
            fos.close();
            ds.close();

        }catch (Exception e){
            System.err.println("Error: " + e);
        }
    }
}
