
package com.example.bodyheatmonitor
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // UI components for user interaction
    private lateinit var usernameEditText: EditText
    private lateinit var ambientTemperatureTextView: TextView
    private lateinit var objectTemperatureTextView: TextView
    private lateinit var fetchDataButton: Button

    // Server configuration
    private val serverUrl = "http://192.168.1.13" // ESP32 server address
    private var isDataDisplayed = false // Flag to track whether data is displayed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        usernameEditText = findViewById(R.id.usernameInput)
        ambientTemperatureTextView = findViewById(R.id.ambientTemp)
        objectTemperatureTextView = findViewById(R.id.objectTemp)
        fetchDataButton = findViewById(R.id.fetchDataButton)

        // Set button click listener
        fetchDataButton.setOnClickListener {
            if (isDataDisplayed) {
                // Reset data if already displayed
                resetDataDisplay()
            } else {
                // Fetch and display data
                val username = usernameEditText.text.toString()
                FetchTemperatureTask(username).execute(serverUrl)
            }
        }
    }

    /**
     * Resets the displayed data to default values and clears user input.
     */
    private fun resetDataDisplay() {
        ambientTemperatureTextView.text = "Ambient Temperature: 0 °C"
        objectTemperatureTextView.text = "Object Temperature: 0 °C"
        usernameEditText.setText("")
        isDataDisplayed = false // Reset the display flag
    }

    /**
     * AsyncTask to fetch temperature data from the server and save it locally.
     */
    private inner class FetchTemperatureTask(private val username: String) : AsyncTask<String, Void, Pair<String, String>?>() {

        override fun doInBackground(vararg urls: String?): Pair<String, String>? {
            return try {
                // Open a connection to the server
                val url = URL(urls[0])
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                // Read the server response
                val scanner = Scanner(connection.inputStream)
                val response = StringBuilder()
                while (scanner.hasNext()) {
                    response.append(scanner.nextLine())
                }
                scanner.close()

                // Parse ambient and object temperatures from the response
                val ambientTempStart = response.indexOf("Ambient Temperature:") + "Ambient Temperature:".length
                val ambientTempEnd = response.indexOf("°C", ambientTempStart)
                val ambientTemp = if (ambientTempStart != -1 && ambientTempEnd != -1) {
                    response.substring(ambientTempStart, ambientTempEnd).trim()
                } else {
                    "N/A"
                }

                val objectTempStart = response.indexOf("Object Temperature:") + "Object Temperature:".length
                val objectTempEnd = response.indexOf("°C", objectTempStart)
                val objectTemp = if (objectTempStart != -1 && objectTempEnd != -1) {
                    response.substring(objectTempStart, objectTempEnd).trim()
                } else {
                    "N/A"
                }

                Pair(ambientTemp, objectTemp)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun onPostExecute(result: Pair<String, String>?) {
            if (result != null) {
                // Update the UI with fetched temperatures
                ambientTemperatureTextView.text = "Ambient Temperature: ${result.first} °C"
                objectTemperatureTextView.text = "Object Temperature: ${result.second} °C"
                isDataDisplayed = true // Mark data as displayed

                // Save the fetched data locally as JSON
                saveTemperatureData(username, result.first, result.second)
            } else {
                // Handle errors and provide a retry option
                Log.e("MainActivity", "Error fetching temperature")
                Snackbar.make(findViewById(android.R.id.content), "Failed to fetch temperature", Snackbar.LENGTH_LONG)
                    .setAction("Retry") { FetchTemperatureTask(username).execute(serverUrl) }
                    .show()
            }
        }
    }

    /**
     * Saves the temperature data as a JSON object in a local file.
     *
     * @param username The username input by the user.
     * @param ambientTemp The fetched ambient temperature.
     * @param objectTemp The fetched object temperature.
     */
    private fun saveTemperatureData(username: String, ambientTemp: String, objectTemp: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val jsonObject = JSONObject().apply {
            put("username", username)
            put("ambientTemperature", ambientTemp)
            put("objectTemperature", objectTemp)
            put("timestamp", timestamp)
        }

        val jsonString = jsonObject.toString()
        try {
            // Save JSON data to a file
            val file = File(filesDir, "data.json")
            file.writeText(jsonString)

            // Log the file path for debugging
            Log.d("MainActivity", "Saved JSON Path: ${file.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
