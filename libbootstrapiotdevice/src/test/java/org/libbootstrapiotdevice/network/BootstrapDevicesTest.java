package org.libbootstrapiotdevice.network;

import android.os.Message;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.libbootstrapiotdevice.BootstrapData;
import org.libbootstrapiotdevice.BootstrapDevice;
import org.libbootstrapiotdevice.DeviceMode;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the bootstrap class. This is the most important class with the protocol
 * implementation therefore a full test including (re)sending and receiving is necessary.
 * To accomplish this, the handler in BootstrapDevices has to be mocked, therefore
 * {@see org.libbootstrapiotdevice.network.MockedHandler} is used.
 * <p/>
 * This test suite simulates receiving of:
 * 1) an unbound device with a correct (generic) key used for encryption.
 * 2) a bound device with a correct (app specific) key used for encryption.
 * 3) a bound device with an unknown key ( == bound to another app).
 * <p/>
 * This test suite tests all three possible outgoing packets:
 * 1) The HELLO packet, send periodically for some duration, packed with the current app nonce.
 * Used for detecting devices in the current network.
 * 2) The BIND packet, used to bind a specific device to this app.
 * 3) The BOOTSTRAP packet, used to send all information necessary for a successful bootstrap.
 */
public class BootstrapDevicesTest implements IUDPNetwork, BootstrapDeviceUpdateListener {
    // This message is generated by the request_wifi_list_tests.cpp RequestList test.
    // Just remove the comment before print_out_array(output_data.data(), output_data.size());
    // The device described here has two reachable networks "wifi1" and "wifi2" with strengths of 100 and 50,
    // both in WPA mode. The device name is "testname", the uid is "ABCDEF".
    // The app nonce value to encrypt the message is "abcdefgh".
    private final static byte msg_encrypted_crc_key_app_secret[] = {'B', 'S', 'T', 'w', 'i', 'f', 'i', '1', (byte) 0x7b, (byte) 0xcf, (byte) 0x0, 'O', (byte) 0x9b, (byte) 0x9b, (byte) 0x83, (byte) 0x86, (byte) 0x13, (byte) 0xab, 'o', (byte) 0x7e, (byte) 0xc2, (byte) 0xcb, (byte) 0x5, 'W', (byte) 0x96, (byte) 0x7b, (byte) 0x92, (byte) 0x8a, (byte) 0x8d, (byte) 0xb2, (byte) 0xa4, (byte) 0xda, (byte) 0xa8, (byte) 0x8b, (byte) 0x5e, (byte) 0xbb, 'O', 'i', (byte) 0x7d, 'G', (byte) 0x20, (byte) 0x0, (byte) 0x14, 'O', '4', (byte) 0x9f, (byte) 0x89, (byte) 0xff, (byte) 0x0, (byte) 0xa7, (byte) 0x92, (byte) 0xd6, (byte) 0xee, 'm', (byte) 0x8e, (byte) 0xd, (byte) 0xfa, (byte) 0x29, 'j', (byte) 0x3e, (byte) 0xff, (byte) 0x8f, 'H', (byte) 0xd3, (byte) 0x81, (byte) 0xf, (byte) 0xcd, (byte) 0xe5, (byte) 0x84, 'z', (byte) 0x3b, (byte) 0xe9, (byte) 0xf1, (byte) 0xd1, (byte) 0x15, (byte) 0x12, (byte) 0x9c, 'X', (byte) 0x7d, (byte) 0x3, (byte) 0xf2, (byte) 0xf8, 'J', (byte) 0xe8, (byte) 0xa3, (byte) 0x7f, (byte) 0x8f, 'z', (byte) 0xb9, (byte) 0x29, (byte) 0xf4, 'H', (byte) 0xf6, (byte) 0xa6, 't', (byte) 0xab, (byte) 0x91, 'G', 'O', (byte) 0xd6, (byte) 0xe3, (byte) 0xcd, 'N', (byte) 0xe6, (byte) 0x8b, (byte) 0x9d, (byte) 0x3e, 'g', 'i', (byte) 0xd6, 'H', (byte) 0xfc, (byte) 0x5d, (byte) 0x3, (byte) 0xf3, (byte) 0xa1, 'R', 'i', (byte) 0xda, (byte) 0x90, (byte) 0x26, (byte) 0x5e, (byte) 0xf, (byte) 0x9e, (byte) 0x20, (byte) 0xc7, (byte) 0xa2, (byte) 0xaf, (byte) 0xa0, (byte) 0x88, (byte) 0x84, 'A', (byte) 0x92, 'O', (byte) 0x9e, (byte) 0x3f, (byte) 0xac, (byte) 0xbf, (byte) 0x29, (byte) 0xb, (byte) 0x83, (byte) 0xa7, '3', (byte) 0x1e, (byte) 0xec, (byte) 0x5e, (byte) 0x5c, (byte) 0xaf, '3', (byte) 0x98, (byte) 0xd8, (byte) 0xac, (byte) 0x22, (byte) 0x7b, (byte) 0x10, (byte) 0xd9, (byte) 0xc2, (byte) 0x80, (byte) 0x1a, (byte) 0x9, (byte) 0xa4, 'x', (byte) 0xc4, (byte) 0x2f, (byte) 0x11, (byte) 0xbf, (byte) 0xa7, (byte) 0x2b, (byte) 0xcb, 'I', (byte) 0xa6, (byte) 0xdc, (byte) 0xcc, (byte) 0x6, (byte) 0xfb, (byte) 0x8f, (byte) 0xc5, (byte) 0xed, (byte) 0x8e, (byte) 0x8a, (byte) 0xf0, (byte) 0xa0, (byte) 0x3d, (byte) 0x29, 'I', (byte) 0x14, (byte) 0xe5, (byte) 0xbc, (byte) 0x5f, (byte) 0xc1, (byte) 0x16, (byte) 0x3, (byte) 0x14, 'l', 'h', 'Y', (byte) 0x14, (byte) 0x9b, (byte) 0xdb, (byte) 0xde, (byte) 0x17, (byte) 0xc1, 'g', (byte) 0xe4, (byte) 0xaf, 's', (byte) 0x40, (byte) 0xda, (byte) 0x2c, (byte) 0xe9, (byte) 0x9e, (byte) 0xf4, (byte) 0x7e, 'B', (byte) 0xfd, (byte) 0xf2, (byte) 0x2f, '2', (byte) 0x24, 'D', (byte) 0xa2, (byte) 0xd3, 'K', (byte) 0x2d, 'j', (byte) 0xc, '4', 'e', 'G', (byte) 0xb4, (byte) 0x96, (byte) 0x1e, (byte) 0x7f, 'S', (byte) 0xaf, 'Y', (byte) 0xf6, (byte) 0xab, (byte) 0xa6, (byte) 0xe1, (byte) 0x5e, (byte) 0x8, 'u', 'z', (byte) 0xda, (byte) 0xd3, (byte) 0xac, 'H', (byte) 0xca, (byte) 0x1, 'g', (byte) 0xfc, (byte) 0xaf, 'i', (byte) 0xa0, (byte) 0x7b, (byte) 0xbc, 'K', 'h', (byte) 0xf0, (byte) 0xd9, (byte) 0xfa, (byte) 0xe4, (byte) 0x19, (byte) 0xfe, (byte) 0xab, (byte) 0xc5, 'h', (byte) 0xaf, (byte) 0xe9, (byte) 0xfb, (byte) 0xfd, 'l', (byte) 0x20, (byte) 0xd7, (byte) 0x5c, 'L', '6', 'J', (byte) 0x6, '1', (byte) 0xc3, (byte) 0xa6, (byte) 0x7f, 'Y', 'L', (byte) 0xd7, (byte) 0x15, (byte) 0xa3, (byte) 0xa8, '4', (byte) 0x82, (byte) 0x12, (byte) 0x85, (byte) 0xe8, 'E', (byte) 0x6, (byte) 0x80, (byte) 0xe9, (byte) 0xbe, (byte) 0xa4, 'J', 'W', (byte) 0x3c, (byte) 0xa7, (byte) 0x7b, 'V', 'H', (byte) 0xdc, 'h', (byte) 0xc6, (byte) 0xb4, (byte) 0x40, (byte) 0x5b, (byte) 0x92, (byte) 0x7f, (byte) 0xd5, (byte) 0xa8, 'B', (byte) 0x6, (byte) 0xf9, (byte) 0x3a, (byte) 0xd3, 'V', (byte) 0x1f, (byte) 0x97, (byte) 0x2b, (byte) 0xab, 'E', (byte) 0x2c, (byte) 0xf4, (byte) 0xd1, 'R', (byte) 0x12, (byte) 0xc, (byte) 0xf6, (byte) 0x3c, (byte) 0xcb, (byte) 0xa2, (byte) 0xec, (byte) 0xec, 'b', 'N', (byte) 0xaa, 'b', 'R', (byte) 0x3d, (byte) 0xc6, (byte) 0x89, (byte) 0x7e, (byte) 0x8b, (byte) 0xeb, (byte) 0x2, (byte) 0xda, (byte) 0x27, 'X', (byte) 0xfd, (byte) 0xb6, (byte) 0xec, (byte) 0xef, (byte) 0x8, (byte) 0xc3, (byte) 0xa8, (byte) 0xbe, 'k', (byte) 0x25, (byte) 0x90, (byte) 0xbb, (byte) 0xdd, (byte) 0x9c, (byte) 0x7e, (byte) 0xd1, (byte) 0x2c, 'A', 'U', (byte) 0xe1, (byte) 0x87, (byte) 0x11, (byte) 0x5d, 'B', (byte) 0x85, 'I', (byte) 0x7e, (byte) 0x24, (byte) 0xc6, (byte) 0xc6, (byte) 0x0, (byte) 0x9c, (byte) 0xcc, (byte) 0xf9, 'G', (byte) 0x1, (byte) 0x1f, (byte) 0xeb, (byte) 0x4, (byte) 0xbc, (byte) 0x3d, (byte) 0xa0, 'o', (byte) 0x1a, (byte) 0x7f, 'F', (byte) 0xca, (byte) 0xb0, (byte) 0xb8, (byte) 0xe4, 'i', 'q', 'u', (byte) 0x5e, (byte) 0xde, (byte) 0x7e, 'O', (byte) 0xd4, (byte) 0xc6, 'A', (byte) 0x4, (byte) 0x8a, (byte) 0xab, (byte) 0x5c, (byte) 0xb3, (byte) 0x8a, 'o', '2', (byte) 0x1f, (byte) 0x40, (byte) 0x9b, (byte) 0xd2, (byte) 0x7b, (byte) 0xf7, (byte) 0xa6, (byte) 0xba, (byte) 0xe9, (byte) 0x2d, (byte) 0x5c, 'p', (byte) 0x92, (byte) 0xe0, 'X', 'E', 'W', (byte) 0x5c, (byte) 0x19, (byte) 0x81, 'M', (byte) 0xbb, (byte) 0x10, (byte) 0xd1, (byte) 0xef, (byte) 0xa, (byte) 0x40, (byte) 0xf9, (byte) 0xfa, (byte) 0x2f, (byte) 0x0, (byte) 0x7c, (byte) 0xfe, (byte) 0x2b, (byte) 0xd3, (byte) 0x2d, 'Y', 'l', (byte) 0x22, (byte) 0x2, (byte) 0xfa, 'D', (byte) 0x0, (byte) 0xe5, (byte) 0xef, (byte) 0x97, (byte) 0xe8, 'H', 'W', (byte) 0xd4, '9', (byte) 0xa0, (byte) 0xfb, (byte) 0xca, (byte) 0xe3, (byte) 0xc7, (byte) 0x7c, (byte) 0xa3, 'c', (byte) 0xb9, (byte) 0x25, 'R', (byte) 0x2e, (byte) 0x90, (byte) 0x98, (byte) 0x2f, (byte) 0x1a, (byte) 0xf2, (byte) 0xb3, (byte) 0xf4, (byte) 0x23, 'K', 'Y', (byte) 0x2b, (byte) 0xfe, 'Q', (byte) 0x7e, (byte) 0xce, 'T', (byte) 0xc5, (byte) 0xc3, (byte) 0xa9, 'N', (byte) 0xfc, 'U', (byte) 0xeb, (byte) 0xbe, (byte) 0x25,};
    private final static byte[] firmware_assumed_app_nonce = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'};
    private final static byte[] firmware_assumed_key = "app_secret\0".getBytes();
    private final static String ownNetworkSSID = "wifi2";
    BootstrapDevices devices;
    BootstrapDeviceUpdateListener changeListener;
    byte[] to_be_send_data;
    int to_be_send_port;
    int flag_deviceUpdated;
    int flag_deviceRemoved;
    boolean flag_deviceRemoveAll;
    boolean flag_deviceChangesFinished;
    // This is part of the mocked handler.
    private Map<Long, Message> queue;

    public static List<byte[]> tokens(byte[] array, int offset, byte delimiter) {
        List<byte[]> byteArrays = new LinkedList<>();
        int begin = offset;

        for (int i = offset; i < array.length; i++) {
            if (array[i] != delimiter) {
                continue;
            }
            byteArrays.add(Arrays.copyOfRange(array, begin, i));
            begin = i + 1;
        }
        byteArrays.add(Arrays.copyOfRange(array, begin, array.length));
        return byteArrays;
    }

    @Before
    public void setUp() throws Exception {
        queue = new TreeMap<>();
        devices = new BootstrapDevices(MockedHandler.createMockedHandler(queue),
                "bound".getBytes(), firmware_assumed_key, ownNetworkSSID);
        devices.setAppNonce(firmware_assumed_app_nonce);
        devices.setNetwork(this);
        devices.addChangeListener(this);
        to_be_send_data = new byte[0];
        to_be_send_port = 0;
        flag_deviceUpdated = -1;
        flag_deviceRemoved = -1;
        flag_deviceRemoveAll = false;
        flag_deviceChangesFinished = false;
    }

    @After
    public void tearDown() throws Exception {
        if (changeListener != null)
            devices.removeChangeListener(changeListener);
        changeListener = null;
//        handlerThread.quit();
//        handlerThread = null;
        devices = null;
    }

    @Test
    public void testRemoveDevicesNotSelected() throws Exception {
        BootstrapDevice d1, d2, d3;
        d1 = new BootstrapDevice("uid1", "name1", null);
        d2 = new BootstrapDevice("uid2", "name2", null);
        d3 = new BootstrapDevice("uid3", "name3", null);
        d1.setSelected(false);
        d2.setSelected(false);
        d3.setSelected(true);
        devices.getDevices().add(d1);
        devices.getDevices().add(d2);
        devices.getDevices().add(d3);
        assertEquals(3, devices.getDevices().size());
        devices.removeDevicesNotSelected();
        assertEquals(1, devices.getDevices().size());
        assertTrue(flag_deviceRemoved >= 0);
    }

    @Test
    public void testClearDevices() throws Exception {
        BootstrapDevice d1, d2, d3;
        d1 = new BootstrapDevice("uid1", "name1", null);
        d2 = new BootstrapDevice("uid2", "name2", null);
        d3 = new BootstrapDevice("uid3", "name3", null);
        devices.getDevices().add(d1);
        devices.getDevices().add(d2);
        devices.getDevices().add(d3);
        assertEquals(3, devices.getDevices().size());
        devices.clearDevices();
        assertEquals(0, devices.getDevices().size());
        assertTrue(flag_deviceRemoveAll);
    }

    /**
     * The parsePacket method is tested by providing an encrypted message
     * and test if the decryption/crc works. A device that uses the unbound key
     * for encryption triggers an outgoing BIND message to be send. This is checked
     * as well. The "received" device information is compared to the data, the
     * firmware test suite generates.
     */
    @Test
    public void testParsePacketUnbound() throws Exception {
        InetSocketAddress receiver = InetSocketAddress.createUnresolved("127.0.0.1", 1111);

        devices.parsePacket(msg_encrypted_crc_key_app_secret,
                msg_encrypted_crc_key_app_secret.length, receiver);

        Message msg = MockedHandler.nextEntry(queue.entrySet(), 1, 0L);
        assertEquals(BootstrapDevices.MSG_BIND_OR_UPDATE, msg.what);

        devices.handleMessage(msg);

        // Expect a device added to the device list.
        assertEquals(1, devices.getDevices().size());
        BootstrapDevice device = devices.getDevices().get(0);
        assertNotNull(device);
        assertEquals(0, flag_deviceUpdated);

        byte[] data = to_be_send_data;

        // Expect an outgoing packet.
        // We expect the header, a crc code, and a command and the app nonce value.
        assertEquals(BootstrapDevices.SEND_PORT, to_be_send_port);

        {
            assertTrue(BootstrapDevices.isHeaderValid(data));
            device.cipherDecrypt(data, BootstrapDevices.protocol_header_len);
            byte crc[] = BootstrapDevices.extractCRC(data);
            byte computed_crc[] = Checksums.CheckSumAsBytes(Checksums.GenerateChecksumCRC16(data, BootstrapDevices.protocol_header_len));
            assertTrue(Arrays.equals(computed_crc, crc));

            ////////// Command //////////
            int code = data[BootstrapDevices.header.length + BootstrapDevices.BST_CHECKSUM_SIZE];
            assertTrue(code <= SendCommandEnum.values().length);
            SendCommandEnum command = SendCommandEnum.values()[code];
            assertEquals(SendCommandEnum.CMD_BIND, command);

            ////////// Content //////////
            //byte send_app_nonce[] = Arrays.copyOfRange(data,header_offset,data.length-header_offset);
            //assertTrue(Arrays.equals(send_app_nonce, firmware_assumed_app_nonce));

            // The first content byte of the bind message is the length of the new key.
            assertEquals(devices.bound_key_len, data[BootstrapDevices.protocol_header_len]);
            byte new_key[] = Arrays.copyOfRange(data, BootstrapDevices.protocol_header_len + 1, data.length);
            byte stored_key[] = Arrays.copyOf(devices.bound_key, devices.bound_key_len);
            assertTrue(Arrays.equals(new_key, stored_key));
        }

        // Check the device properties according to the  generated device in the
        // request_wifi_list_tests.cpp RequestList test.
        assertEquals("testname", device.device_name);
        assertEquals("ABCDEF", device.uid);
        assertEquals(DeviceMode.Binding, device.getMode());
        assertEquals(DeviceState.STATE_OK, device.getState());
        // If a device reports wifi networks and one of the network is the current one,
        // then getWirelessStrength() will report the wifi signal strength.
        assertNotNull(device.getWirelessNetwork());
        assertEquals(ownNetworkSSID, device.getWirelessNetwork().ssid);
        assertEquals(50, device.getWirelessNetwork().getStrength());
    }

    @Test
    public void testParsePacketBound() throws Exception {
        devices.swapBoundUnboundKeys();

        InetSocketAddress receiver = InetSocketAddress.createUnresolved("127.0.0.1", 1111);

        devices.parsePacket(msg_encrypted_crc_key_app_secret,
                msg_encrypted_crc_key_app_secret.length, receiver);

        Message msg = MockedHandler.nextEntry(queue.entrySet(), 1, 0L);
        assertEquals(BootstrapDevices.MSG_BIND_OR_UPDATE, msg.what);

        devices.handleMessage(msg);

        // Expect a device added to the device list.
        assertEquals(1, devices.getDevices().size());
        BootstrapDevice device = devices.getDevices().get(0);
        assertEquals(DeviceMode.Bound, device.getMode());
        assertEquals(0, flag_deviceUpdated);
    }

    @Test
    public void testDetectDevices() throws Exception {
        devices.detectDevices(500, 3);
        devices.setAppNonce(firmware_assumed_app_nonce);

        // Check if messages are added to handler queue
        Message msg, finishedMsg;
        msg = MockedHandler.nextEntry(queue.entrySet(), 4, 0L);
        assertEquals(BootstrapDevices.MSG_DETECT, msg.what);
        msg = MockedHandler.nextEntry(queue.entrySet(), 3, 500L);
        assertEquals(BootstrapDevices.MSG_DETECT, msg.what);
        msg = MockedHandler.nextEntry(queue.entrySet(), 2, 1000L);
        assertEquals(BootstrapDevices.MSG_DETECT, msg.what);
        finishedMsg = MockedHandler.nextEntry(queue.entrySet(), 1, 1500L);
        assertEquals(BootstrapDevices.MSG_DETECT_FINISHED, finishedMsg.what);

        // This will generate a HELLO message
        devices.handleMessage(msg);

        // Check hello message
        assertEquals(BootstrapDevices.SEND_PORT, to_be_send_port);
        byte[] data = to_be_send_data;

        {
            assertTrue(BootstrapDevices.isHeaderValid(data));
            byte crc[] = BootstrapDevices.extractCRC(data);
            byte computed_crc[] = Checksums.CheckSumAsBytes(Checksums.GenerateChecksumCRC16(data, BootstrapDevices.protocol_header_len));
            assertTrue(Arrays.equals(computed_crc, crc));

            ////////// Command //////////
            int code = data[BootstrapDevices.header.length + BootstrapDevices.BST_CHECKSUM_SIZE];
            assertTrue(code <= SendCommandEnum.values().length);
            SendCommandEnum command = SendCommandEnum.values()[code];
            assertEquals(SendCommandEnum.CMD_HELLO, command);

            ////////// Content //////////
            byte send_app_nonce[] = Arrays.copyOfRange(data, BootstrapDevices.protocol_header_len, data.length);
            assertTrue(Arrays.equals(send_app_nonce, firmware_assumed_app_nonce));
        }
    }

    @Test
    public void testBootstrapDevicesNoBoundDevices() throws Exception {
        BootstrapDevice d1, d2, d3;
        d1 = new BootstrapDevice("uid1", "name1", null);
        d1.setMode(DeviceMode.Unbound);
        d2 = new BootstrapDevice("uid2", "name2", null);
        d2.setMode(DeviceMode.Binding);
        d3 = new BootstrapDevice("uid3", "name3", null);
        d3.setMode(DeviceMode.NotInRange);
        devices.getDevices().add(d1);
        devices.getDevices().add(d2);
        devices.getDevices().add(d3);
        assertEquals(3, devices.getDevices().size());

        BootstrapData bootstrapData = new BootstrapData();
        bootstrapData.setWifiData("test_wifi_ssid", "test_wifi_pwd");

        devices.bootstrapDevices(500, 3, bootstrapData);
        devices.setAppNonce(firmware_assumed_app_nonce);

        // Check if messages are added to handler queue
        Message msg, finishedMsg;
        msg = MockedHandler.nextEntry(queue.entrySet(), 4, 0L);
        assertEquals(BootstrapDevices.MSG_BOOTSTRAP, msg.what);
        msg = MockedHandler.nextEntry(queue.entrySet(), 3, 500L);
        assertEquals(BootstrapDevices.MSG_BOOTSTRAP, msg.what);
        msg = MockedHandler.nextEntry(queue.entrySet(), 2, 1000L);
        assertEquals(BootstrapDevices.MSG_BOOTSTRAP, msg.what);
        finishedMsg = MockedHandler.nextEntry(queue.entrySet(), 1, 1500L);
        assertEquals(BootstrapDevices.MSG_BOOTSTRAP_FINISHED, finishedMsg.what);

        // This will NOT generate a BOOTSTRAP message
        devices.handleMessage(msg);
        assertEquals(-1, flag_deviceUpdated);

        assertEquals(0, to_be_send_data.length);
    }

    @Test
    public void testBootstrapDevices() throws Exception {
        byte device_nonce[] = {'d', 'e', 'v', 'i', 'c', 'e'};
        BootstrapDevice d1;
        d1 = new BootstrapDevice("uid1", "name1", null);
        d1.updateState(DeviceMode.Bound, DeviceState.STATE_OK, null,
                "", device_nonce, firmware_assumed_key, firmware_assumed_key.length);
        devices.getDevices().add(d1);
        assertEquals(1, devices.getDevices().size());

        BootstrapData bootstrapData = new BootstrapData();
        bootstrapData.setWifiData("test_wifi_ssid", "test_wifi_pwd");
        bootstrapData.addAdditionalData("testkey", "testvalue");

        devices.bootstrapDevices(500, 3, bootstrapData);
        devices.setAppNonce(firmware_assumed_app_nonce);

        // Check if messages are added to handler queue
        Message msg, finishedMsg;
        msg = MockedHandler.nextEntry(queue.entrySet(), 4, 0L);
        assertEquals(BootstrapDevices.MSG_BOOTSTRAP, msg.what);
        msg = MockedHandler.nextEntry(queue.entrySet(), 3, 500L);
        assertEquals(BootstrapDevices.MSG_BOOTSTRAP, msg.what);
        msg = MockedHandler.nextEntry(queue.entrySet(), 2, 1000L);
        assertEquals(BootstrapDevices.MSG_BOOTSTRAP, msg.what);
        finishedMsg = MockedHandler.nextEntry(queue.entrySet(), 1, 1500L);
        assertEquals(BootstrapDevices.MSG_BOOTSTRAP_FINISHED, finishedMsg.what);

        // This will generate a BOOTSTRAP message
        devices.handleMessage(msg);

        // Expect a device added to the device list.
        assertEquals(1, devices.getDevices().size());
        BootstrapDevice device = devices.getDevices().get(0);
        assertNotNull(device);

        // Check bootstrap message
        assertEquals(BootstrapDevices.SEND_PORT, to_be_send_port);
        byte[] data = to_be_send_data;

        {
            assertTrue(BootstrapDevices.isHeaderValid(data));
            device.cipherDecrypt(data, BootstrapDevices.protocol_header_len);
            byte crc[] = BootstrapDevices.extractCRC(data);
            byte computed_crc[] = Checksums.CheckSumAsBytes(Checksums.GenerateChecksumCRC16(data, BootstrapDevices.protocol_header_len));
            assertTrue(Arrays.equals(computed_crc, crc));

            ////////// Command //////////
            int code = data[BootstrapDevices.header.length + BootstrapDevices.BST_CHECKSUM_SIZE];
            assertTrue(code <= SendCommandEnum.values().length);
            SendCommandEnum command = SendCommandEnum.values()[code];
            assertEquals(SendCommandEnum.CMD_SET_DATA, command);

            ////////// Content //////////
            List<byte[]> tokens = tokens(data, BootstrapDevices.protocol_header_len, (byte) 0);
            assertEquals(4, tokens.size());
            assertTrue(Arrays.equals("test_wifi_ssid".getBytes(), tokens.get(0)));
            assertTrue(Arrays.equals("test_wifi_pwd".getBytes(), tokens.get(1)));
            assertTrue(Arrays.equals("testkey\ttestvalue\t".getBytes(), tokens.get(2)));
        }
    }

    /**
     * We implement the IUDPNetwork interface in this test fixture and copy the outgoing
     * data buffer to a field variable to perform tests on those packets.
     *
     * @param sendPort The port the packet should be send
     * @param address  The address to send the packet to.
     * @param data     The data to send.
     * @return Return true if the packet could be handed over to the network stack.
     */
    @Override
    public boolean send(int sendPort, InetAddress address, byte[] data) {
        to_be_send_data = data;
        to_be_send_port = sendPort;
        return true;
    }

    /**
     * @return Return false if the network is not ready. For this test fixture, the
     * network is always ready of course.
     */
    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void deviceUpdated(int index, boolean added) {
        flag_deviceUpdated = index;
    }

    @Override
    public void deviceRemoved(int index) {
        flag_deviceRemoved = index;
    }

    @Override
    public void deviceRemoveAll() {
        flag_deviceRemoveAll = true;
    }

    @Override
    public void deviceChangesFinished() {
        flag_deviceChangesFinished = true;
    }
}