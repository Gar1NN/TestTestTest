package com.example.testtesttest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.io.ModbusTransaction;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.net.TCPMasterConnection;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {
    static int ref = 0x0708;
    static int count = 1;
    static int port = 50000;
    static InetAddress inetAddress = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Thread(new Task()).start();

    }

    public static class Task extends Thread{
        public void run(){
            TCPMasterConnection con = null;
            try {
                inetAddress = InetAddress.getByName("46.48.42.174");
            } catch (UnknownHostException unknownHostException) {
                unknownHostException.printStackTrace();
            }
            // Open the connection
            con = new TCPMasterConnection(inetAddress);
            con.setPort(port);
            try {
                con.connect();
                Log.d("TCPMasterConnection", "You are connected");
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            Log.d("MODBUS", "connection normalno");


            ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(ref, count);
            req.setUnitID(1);
            req.setHeadless();
            Log.d("req", req.getHexMessage());


            String crc_in_string = getCRC(req.getHexMessage()).replace(" ", "");
            Log.d("crc", crc_in_string);
            Integer crc = 0;
            crc = Integer.parseInt(crc_in_string, 16);

            OurRequest anotherReaquest = new OurRequest(ref, count);
            anotherReaquest.setCRC(crc);
            anotherReaquest.setHeadless();
            anotherReaquest.setUnitID(1);
            Log.d("OurRequest", anotherReaquest.getHexMessage());

            ModbusTransaction trans = new ModbusTCPTransaction(con);
            trans.setRetries(0);

            trans.setRequest(anotherReaquest);
            try {
                trans.execute();
            } catch (ModbusException e) {
                e.printStackTrace();
            }

            ModbusResponse response = trans.getResponse();
            response.getHexMessage();


            con.close();
        }

        public static String getCRC(String data) {
            data = data.replace(" ", "");
            int len = data.length();
            if (!(len % 2 == 0)) {
                return "0000";
            }
            int num = len / 2;
            byte[] para = new byte[num];
            for (int i = 0; i < num; i++) {
                int value = Integer.valueOf(data.substring(i * 2, 2 * (i + 1)), 16);
                para[i] = (byte) value;
            }
            return getCRC(para);
        }


        /**
         *Calculate CRC16 check code
         *
         * @param bytes
         *Byte array
         *@ return {@ link string} check code
 * @since 1.0
         */
        public static String getCRC(byte[] bytes) {
            //CRC registers are all 1
            int CRC = 0x0000ffff;
            //Polynomial check value
            int POLYNOMIAL = 0x0000a001;
            int i, j;
            for (i = 0; i < bytes.length; i++) {
                CRC ^= ((int) bytes[i] & 0x000000ff);
                for (j = 0; j < 8; j++) {
                    if ((CRC & 0x00000001) != 0) {
                        CRC >>= 1;
                        CRC ^= POLYNOMIAL;
                    } else {
                        CRC >>= 1;
                    }
                }
            }
            //Result converted to hex
            String result = Integer.toHexString(CRC).toUpperCase();
            if (result.length() != 4) {
                StringBuffer sb = new StringBuffer("0000");
                result = sb.replace(4 - result.length(), 4, result).toString();
            }
            //High position in the front position in the back
            //return result.substring(2, 4) + " " + result.substring(0, 2);
            //Exchange high low, low in front, high in back
            return result.substring(2, 4) + " " + result.substring(0, 2);
        }
    }
}