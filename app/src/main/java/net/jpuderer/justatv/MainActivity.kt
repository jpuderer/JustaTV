package net.jpuderer.justatv

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.TextView
import com.google.android.things.pio.PeripheralManagerService
import com.google.android.things.pio.UartDevice
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import java.io.IOException
import com.google.android.things.pio.UartDeviceCallback
import android.text.method.ScrollingMovementMethod
import android.view.KeyEvent
import com.google.android.things.contrib.driver.button.Button

class MainActivity : Activity() {
    private val TAG = "JustaTV"
    private val UART_DEVICE_NAME = "UART0" // UART1 exists, but is connect to BT

    private val HDMI1_BUTTON_GPIO = "BCM17"
    private val HDMI2_BUTTON_GPIO = "BCM27"
    private val HDMI3_BUTTON_GPIO = "BCM22"

    private lateinit var mDevice: UartDevice
    private lateinit var mConsoleView : TextView
    private var mHandler : Handler = Handler()

    private lateinit var mDisableOsdRunnable : Runnable
    private lateinit var mHdmi1InputDriver : ButtonInputDriver
    private lateinit var mHdmi2InputDriver : ButtonInputDriver
    private lateinit var mHdmi3InputDriver : ButtonInputDriver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // This runnable turns off the annoying On Screen Display (OSD).
        // Since we don't know when the TV is on, we just send this sequence
        // every minute.
        mDisableOsdRunnable = Runnable() {
            val powerStatusCmd = "kl 00 00\n".toByteArray()
            writeUartData(mDevice, powerStatusCmd)
            mHandler.postDelayed(mDisableOsdRunnable,60000)
        }

        try {
            val manager = PeripheralManagerService()
            mDevice = manager.openUartDevice(UART_DEVICE_NAME)
        } catch (e: IOException) {
            Log.w(TAG, "Unable to access UART device", e)
        }

        try {
            mHdmi1InputDriver = ButtonInputDriver(
                    HDMI1_BUTTON_GPIO,
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_1)
            mHdmi1InputDriver.register()
            mHdmi2InputDriver = ButtonInputDriver(
                    HDMI2_BUTTON_GPIO,
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_2)
            mHdmi2InputDriver.register()
            mHdmi3InputDriver = ButtonInputDriver(
                    HDMI3_BUTTON_GPIO,
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_3)
            mHdmi3InputDriver.register()
        } catch (e: IOException) {
            Log.e(TAG, "Error configuring GPIO pins", e)
        }

    }

    override fun onStart() {
        super.onStart()
        // Begin listening for interrupt events
        mDevice.registerUartDeviceCallback(mUartCallback)

        mHandler.postDelayed(mDisableOsdRunnable, 60000)
    }

    override fun onStop() {
        super.onStop()
        // Interrupt events no longer necessary
        mDevice.unregisterUartDeviceCallback(mUartCallback)
    }

    private val mUartCallback = object : UartDeviceCallback() {
        override fun onUartDeviceDataAvailable(uart: UartDevice): Boolean {
            // Read available data from the UART device
            try {
                readUartBuffer(uart)
            } catch (e: IOException) {
                Log.w(TAG, "Unable to access UART device", e)
            }

            // Continue listening for more interrupts
            return true
        }

        override fun onUartDeviceError(uart: UartDevice?, error: Int) {
            Log.w(TAG, uart.toString() + ": Error event " + error)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            mDevice.close()
        } catch (e: IOException) {
            Log.w(TAG, "Unable to close UART device", e)
        }

        try {
            mHdmi1InputDriver.unregister();
            mHdmi1InputDriver.close();
            mHdmi2InputDriver.unregister();
            mHdmi2InputDriver.close();
            mHdmi3InputDriver.unregister();
            mHdmi3InputDriver.close();
        } catch (e : IOException) {
            Log.e(TAG, "Error closing Button driver", e);
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val powerStatusCmd : ByteArray

        if (keyCode == KeyEvent.KEYCODE_1) {
            powerStatusCmd = "xb 00 90\n".toByteArray()
            Log.d(TAG, "Switching to HDMI1");
        } else if (keyCode == KeyEvent.KEYCODE_2) {
            powerStatusCmd = "xb 00 91\n".toByteArray()
            Log.d(TAG, "Switching to HDMI2");
        } else if (keyCode == KeyEvent.KEYCODE_3) {
            powerStatusCmd = "xb 00 92\n".toByteArray()
            Log.d(TAG, "Switching to HDMI3");
        } else {
            Log.e(TAG, "Unknown keycode: " + keyCode)
            return true
        }
        writeUartData(mDevice, powerStatusCmd)
        return true
    }

    @Throws(IOException::class)
    fun configureUartFrame(uart: UartDevice) {
        // Configure the UART port
        uart.setBaudrate(9600)
        uart.setDataSize(8)
        uart.setParity(UartDevice.PARITY_NONE)
        uart.setStopBits(1)
    }

    @Throws(IOException::class)
    fun writeUartData(uart: UartDevice, buffer: ByteArray) {
        uart.write(buffer, buffer.size);
    }

    @Throws(IOException::class)
    fun readUartBuffer(uart: UartDevice) {
        // Maximum amount of data to read at one time
        var buffer = ByteArray(1024)
        do {
            var count = uart.read(buffer, buffer.size)
            if (count <= 0) break
            Log.d(TAG,  "Read from TV: " + String(buffer, 0, count, Charsets.UTF_8) + "\n")
        } while (true)
    }
}
