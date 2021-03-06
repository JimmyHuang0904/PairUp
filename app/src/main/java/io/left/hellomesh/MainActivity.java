package io.left.hellomesh;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import io.left.rightmesh.android.AndroidMeshManager;
import io.left.rightmesh.android.MeshService;
import io.left.rightmesh.id.MeshID;
import io.left.rightmesh.mesh.MeshManager;
import io.left.rightmesh.mesh.MeshStateListener;
import io.left.rightmesh.util.MeshUtility;
import io.left.rightmesh.util.RightMeshException;
import io.reactivex.functions.Consumer;

import static io.left.rightmesh.mesh.MeshManager.DATA_RECEIVED;
import static io.left.rightmesh.mesh.MeshManager.PEER_CHANGED;
import static io.left.rightmesh.mesh.MeshManager.REMOVED;

/**
 * Main activity of the app. Should only be on the user's screen when the user has created a group
 * Or is currently in a group
 */
public class MainActivity extends FragmentActivity implements MeshStateListener {
    // Port to bind app to.
    private static final int HELLO_PORT = 9090;

    // MeshManager instance - interface to the mesh network.
    AndroidMeshManager mm = null;

    // Object to send messages over the mesh. Initialized in onCreate
    MessageSender messageSender = null;

    // Object to parse and handle message coming over the mesh. Initialized in onCreate
    MessageHandler messageHandler = null;

    // Keep track of users connected to the mesh
    PeerStore peerStore = null;

    // Keep track of data related to the device's user
    UserData userData = null;

    ListAdapter mAdapter = null;

    private String getUsername() {
        // Intent from first activity
        //TextView txtStatus = (TextView) findViewById(R.id.txtStatus);

        // Intent intent = getIntent();
        return getIntent().getStringExtra("username");
    }

    /**
     * Called when app first opens, initializes {@link AndroidMeshManager} reference (which will
     * start the {@link MeshService} if it isn't already running.
     *
     * @param savedInstanceState passed from operating system
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userData = new UserData(this.getUsername());

        setContentView(R.layout.activity_main);

        mm = AndroidMeshManager.getInstance(MainActivity.this, MainActivity.this);
        messageSender = new MessageSender(mm, HELLO_PORT);
        peerStore = new PeerStore();
        messageHandler = new MessageHandler(peerStore);
        mAdapter = new ListAdapter(this);

        String groupName = getIntent().getExtras().getString("group_name");
        if (groupName != null && !groupName.equals("")) {
            userData.setGroup(getIntent().getExtras().getString("group_name"));
            Toast.makeText(this, "GROUP ADD SUCCESSFUL", Toast.LENGTH_SHORT).show();
            mAdapter.addSectionHeaderItem("Acquiring Groups...");
        }
    }

    /**
     * Called when activity is on screen.
     */
    @Override
    protected void onResume() {
        try {
            super.onResume();
            mm.resume();
        } catch (MeshService.ServiceDisconnectedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when the app is being closed (not just navigated away from). Shuts down
     * the {@link AndroidMeshManager} instance.
     */
    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();
            mm.stop();
        } catch (MeshService.ServiceDisconnectedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called by the {@link MeshService} when the mesh state changes. Initializes mesh connection
     * on first call.
     *
     * @param uuid our own user id on first detecting
     * @param state state which indicates SUCCESS or an error code
     */
    @Override
    public void meshStateChanged(MeshID uuid, int state) {
        if (state == MeshStateListener.SUCCESS) {
            try {
                // Binds this app to MESH_PORT.
                // This app will now receive all events generated on that port.
                mm.bind(HELLO_PORT);

                // Subscribes handlers to receive events from the mesh.
                mm.on(DATA_RECEIVED, new Consumer() {
                    @Override
                    public void accept(Object o) throws Exception {
                        handleDataReceived((MeshManager.RightMeshEvent) o);
                    }
                });
                mm.on(PEER_CHANGED, new Consumer() {
                    @Override
                    public void accept(Object o) throws Exception {
                        handlePeerChanged((MeshManager.RightMeshEvent) o);
                    }
                });

                // If you are using Java 8 or a lambda backport like RetroLambda, you can use
                // a more concise syntax, like the following:
                // mm.on(PEER_CHANGED, this::handlePeerChanged);
                // mm.on(DATA_RECEIVED, this::dataReceived);

                // Enable buttons now that mesh is connected.
                Button btnConfigure = (Button) findViewById(R.id.btnConfigure);
                Button btnSend = (Button) findViewById(R.id.btnHello);
                btnConfigure.setEnabled(true);
                btnSend.setEnabled(true);

                // initialized, so say that the user is now connected
                userData.setConnected();
            } catch (RightMeshException e) {
/*                String status = "Error initializing the library" + e.toString();
                Toast.makeText(getApplicationContext(), status, Toast.LENGTH_SHORT).show();
                TextView txtStatus = (TextView) findViewById(R.id.txtStatus);
                txtStatus.setText(status);
                return;*/
            }
        }

/*        // Update display on successful calls (i.e. not FAILURE or DISABLED).
        if (state == MeshStateListener.SUCCESS || state == MeshStateListener.RESUME) {
            updateStatus();
        }*/
    }

    /**
     * Update the {@link TextView} with a list of all peers.
     */
/*    private void updateStatus() {
        String status = "uuid: " + mm.getUuid().toString() + "\npeers:\n";
        for (MeshID peer : peerStore.getAllUuids()) {
            status += peer.toString() + "\n";
        }
        TextView txtStatus = (TextView) findViewById(R.id.txtStatus);
        txtStatus.setText(status);
    }*/

    private void updateList() {
        ListView listView = (ListView) findViewById(R.id.groupList);
        // Populate lists with groups and names here
        mAdapter.clear();
        if (peerStore.getAllUuids().isEmpty()){
            mAdapter.addSectionHeaderItem("Acquiring Groups...");
        } else {
            for (String groupName : peerStore.getAllGroupNames()) {
                mAdapter.addSectionHeaderItem("Group: " + groupName);
                for (String peerName : peerStore.getPeerNamesInGroup(groupName)) {
                    mAdapter.addItem(peerName);
                }
            }
        }

        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {

                String itemText = (String) parent.getItemAtPosition(position);

                String groupName = null;
                if (itemText.startsWith("Group")) {
                    groupName = itemText.substring(7);
                }else {
                    if (peerStore.getPeer(itemText) != null && peerStore.getPeer(itemText).getGroupName() != null) {
                        groupName = peerStore.getPeer(itemText).getGroupName();
                    }else {
                        return;
                    }
                }
                userData.setGroup(groupName);
                try {
                    messageSender.sendGroupToMany(peerStore.getAllUuids(), groupName);
                } catch (RightMeshException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Handles incoming data events from the mesh - toasts the contents of the data.
     *
     * @param e event object from mesh
     */
    private void handleDataReceived(MeshManager.RightMeshEvent e) {
        final MeshManager.DataReceivedEvent event = (MeshManager.DataReceivedEvent) e;
        messageHandler.handleMessage(new String(event.data));

        // TODO: remove the toasts once we dont need them
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateList();
                // Toast data contents.
                String message = new String(event.data);
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();

                // Play a notification.
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(MainActivity.this, notification);
                r.play();
            }
        });
    }

    /**
     * Handles peer update events from the mesh - maintains a list of peers and updates the display.
     *
     * @param e event object from mesh
     */
    private void handlePeerChanged(MeshManager.RightMeshEvent e) {
        // Update peer list.
        MeshManager.PeerChangedEvent event = (MeshManager.PeerChangedEvent) e;
        if (event.state != REMOVED && !peerStore.containsPeer(event.peerUuid)) {
            peerStore.addPeer(event.peerUuid);
            try {
                // tell the new person your name
                messageSender.sendName(event.peerUuid, this.userData.getName());

                // if you're in a group, tell the new person you're in that group
                boolean isInGroup = this.userData.getGroup() != null;
                if (isInGroup) {
                    messageSender.sendGroupToIndividual(event.peerUuid, this.userData.getGroup());
                }
            }catch (RightMeshException exception) {
                exception.printStackTrace();
            }

        } else if (event.state == REMOVED){
            peerStore.removePeer(event.peerUuid);
            // SOMEBODY DISCONNECTED OH NO!!!!
            // if theyre part of your group, then you should be alarmed
            if (userData.getGroup() != null && peerStore.getPeer(event.peerUuid).getGroupName().equals(userData.getGroup())) {
                Intent intent = new Intent(getApplicationContext(), DisconnectActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("message", peerStore.getPeer(event.peerUuid).getName() + " has been disconnected.");
                startActivity(intent);
            }
        }

/*        // Update display.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateStatus();
            }
        });*/
    }

    /**
     * Sends "hello" to all known peers.
     *
     * @param v calling view
     */
    public void sendHello(View v) throws RightMeshException {
        String ownName = userData.getName() != null ? userData.getName() : mm.getUuid().toString();
        for(MeshID receiver : peerStore.getAllUuids()) {
            String theirName = peerStore.getPeer(receiver).getName() != null ? peerStore.getPeer(receiver).getName(): receiver.toString();
            String msg = String.format("Hello to: %s from %s", theirName, ownName);

            MeshUtility.Log(this.getClass().getCanonicalName(), "MSG: " + msg);
            byte[] testData = msg.getBytes();
            mm.sendDataReliable(receiver, HELLO_PORT, testData);
        }
    }

    /**
     * Open mesh settings screen.
     *
     * @param v calling view
     */
    public void configure(View v)
    {
        try {
            mm.showSettingsActivity();
        } catch(RightMeshException ex) {
            MeshUtility.Log(this.getClass().getCanonicalName(), "Service not connected");
        }
    }
}

