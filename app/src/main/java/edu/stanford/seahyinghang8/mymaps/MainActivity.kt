package edu.stanford.seahyinghang8.mymaps

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import edu.stanford.seahyinghang8.mymaps.models.Place
import edu.stanford.seahyinghang8.mymaps.models.UserMap
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*

private const val TAG = "MainActivity"
private const val REQUEST_CODE = 3134
private const val FILENAME = "UserMaps.data"
const val EXTRA_POSITION = "EXTRA_POSITION"
const val EXTRA_USER_MAP = "EXTRA_USER_MAP"
const val EXTRA_SHOW_INS = "EXRA_SHOW_INSTRUCTION"
const val EXTRA_MAP_RESULT = "EXTRA_MAP_RESULT"

class MainActivity : AppCompatActivity() {

    private lateinit var userMaps: MutableList<UserMap>
    private lateinit var mapAdapter: MapsAdapter
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userMaps = deserializeUserMaps(this).toMutableList()
        // Set up layout manager on the recycler view
        rvMap.layoutManager = LinearLayoutManager(this)
        // Set adapter on the recycler view
        mapAdapter = MapsAdapter(this, userMaps, object : MapsAdapter.OnClickListener {
            override fun onItemClick(position: Int) {
                val intent = Intent(this@MainActivity, DisplayMapActivity::class.java)
                intent.putExtra(EXTRA_POSITION, position)
                intent.putExtra(EXTRA_USER_MAP, userMaps[position])
                intent.putExtra(EXTRA_SHOW_INS, false)
                startActivityForResult(intent, REQUEST_CODE)
            }

            override fun onItemLongClick(position: Int) {
                showEditMapDialog(position)
            }
        })
        rvMap.adapter = mapAdapter

        fabCreateMap.setOnClickListener {
            showEditMapDialog(null)
        }

        sharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE)

        // Show a snackbar with instructions if necessary
        if (userMaps.size > 0 && sharedPreferences.getBoolean("ShowMainSnackbar", true)) {
            Snackbar.make(mainConstraint, "Long click to edit entries.", Snackbar.LENGTH_INDEFINITE)
                .setAction("OK", {})
                .show()
            sharedPreferences.edit().putBoolean("ShowMainSnackbar", false).apply()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.i(TAG, "Receiving results from DisplayMapActivity")
            val (position, places) = data?.getSerializableExtra(EXTRA_MAP_RESULT) as Pair<Int, List<Place>>
            Log.i(TAG, "$places")
            userMaps[position].places = places
            mapAdapter.notifyItemChanged(position)
            serializeUserMaps(this, userMaps)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showEditMapDialog(position: Int?) {
        val mapFormView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_map, null)
        val positiveText = if (position == null) "Create" else "Save"
        val negativeText = if (position == null) "Cancel" else "Delete"
        val dialog =
            AlertDialog.Builder(this)
                .setView(mapFormView)
                .setNegativeButton(negativeText, null)
                .setPositiveButton(positiveText, null)
                .show()

        val mapTitle = mapFormView.findViewById<EditText>(R.id.etMapTitle)
        val mapDescription = mapFormView.findViewById<EditText>(R.id.etMapDescription)

        if (position != null) {
            mapTitle.setText(userMaps[position].title)
            mapDescription.setText(userMaps[position].description)
        }

        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener {
            if (position != null) {
                userMaps.removeAt(position)
                mapAdapter.notifyItemRemoved(position)
                mapAdapter.notifyItemRangeChanged(position, mapAdapter.itemCount)
                serializeUserMaps(this@MainActivity, userMaps)
                Toast.makeText(this, "Map ${mapTitle.text} deleted.", Toast.LENGTH_LONG).show()
            }
            dialog.dismiss()
        }

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val title = mapTitle.text.toString()
            val description = mapDescription.text.toString()
            if (title.trim().isEmpty() || description.trim().isEmpty()) {
                Toast.makeText(this, "Map must have a non-empty title and description", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (position == null) {
                // Add new map to user maps
                userMaps.add(
                    UserMap(
                        mapTitle.text.toString(),
                        mapDescription.text.toString(),
                        emptyList()
                    )
                )
                mapAdapter.notifyItemInserted(userMaps.lastIndex)
                // Navigate to display map activity
                val intent = Intent(this@MainActivity, DisplayMapActivity::class.java)
                intent.putExtra(EXTRA_POSITION, userMaps.lastIndex)
                intent.putExtra(EXTRA_USER_MAP, userMaps.last())
                // Tell the map to show the instruction snackbar
                intent.putExtra(EXTRA_SHOW_INS, sharedPreferences.getBoolean("ShowMapSnackbar", true))
                sharedPreferences.edit().putBoolean("ShowMapSnackbar", false).apply()
                startActivityForResult(intent, REQUEST_CODE)
            } else {
                // Update user maps
                userMaps[position].title = mapTitle.text.toString()
                userMaps[position].description = mapDescription.text.toString()
                mapAdapter.notifyItemChanged(position)
            }

            serializeUserMaps(this@MainActivity, userMaps)
            dialog.dismiss()
        }
    }

    private fun serializeUserMaps(context: Context, userMaps: List<UserMap>) {
        Log.i(TAG, "serializeUserMaps")
        ObjectOutputStream(FileOutputStream(getDataFile(context))).use { it.writeObject(userMaps) }
    }

    private fun deserializeUserMaps(context: Context) : List<UserMap> {
        Log.i(TAG, "deserializeUserMaps")
        val dataFile = getDataFile(context)
        if (!dataFile.exists()) {
            Log.i(TAG, "Data file does not exist yet")
            return emptyList()
        }
        ObjectInputStream(FileInputStream(dataFile)).use { return it.readObject() as List<UserMap> }
    }

    private fun getDataFile(context: Context) : File {
        Log.i(TAG, "Getting file from directory ${context.filesDir}")
        return File(context.filesDir, FILENAME)
    }
}