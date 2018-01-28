package io.left.hellomesh;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import android.app.Activity;

import io.left.rightmesh.id.MeshID;

import java.util.HashSet;
import java.util.HashMap;

import io.left.rightmesh.android.MeshService;
import io.left.rightmesh.android.AndroidMeshManager;
import io.left.rightmesh.mesh.MeshManager;
import io.left.rightmesh.mesh.MeshStateListener;
import io.left.rightmesh.util.MeshUtility;
import io.left.rightmesh.util.RightMeshException;

import io.reactivex.functions.Consumer;

import static io.left.rightmesh.mesh.MeshManager.DATA_RECEIVED;
import static io.left.rightmesh.mesh.MeshManager.PEER_CHANGED;
import static io.left.rightmesh.mesh.MeshManager.REMOVED;

public class ParamedicActivity extends Activity implements MeshStateListener {
    private class UserInfo {
       String name;
       String role;

       public UserInfo(String name, String role) {
          this.name = name;
          this.role = role;
       }
    }

    private static final int PORT = 9876;
    AndroidMeshManager mm = null;
    HashSet<MeshID> users = new HashSet<>();
    HashMap<MeshID, UserInfo> userInfo = new HashMap<MeshID, UserInfo>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paramedic);

        mm = AndroidMeshManager.getInstance(ParamedicActivity.this, ParamedicActivity.this);
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
                mm.bind(PORT);

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

            } catch (RightMeshException e) {
                return;
            }
        }
    }

    private void handleDataReceived(MeshManager.RightMeshEvent e) {
        final MeshManager.DataReceivedEvent event = (MeshManager.DataReceivedEvent) e;
        String peerUuid = String.valueOf(event.peerUuid);
        String data = new String(event.data);
        String[] args = data.split(";");
        System.out.println("event from Patient: " + data);

        if (args[0].equals("receiveInfo")) {
            String name = args[1];
            String role = args[2];

            if (role.equals("patient")) {
                userInfo.put(event.peerUuid, new UserInfo(name, role));

                TextView patientList = (TextView) findViewById(R.id.patientData);
                patientList.append("Peer Id: " + peerUuid + "\n" + data + "\n\n");
            }
        } else if (args[0].equals("getInfo")) {
            try {
                mm.sendDataReliable(event.peerUuid, PORT, "receiveInfo;andy;paramedic;".getBytes());
            } catch (RightMeshException re) {}
        }
    }

    private void handlePeerChanged(MeshManager.RightMeshEvent e) {
        // Update peer list.
        MeshManager.PeerChangedEvent event = (MeshManager.PeerChangedEvent) e;
        if (event.state != REMOVED && !users.contains(event.peerUuid)) {
            users.add(event.peerUuid);
            System.out.println("peer: " + String.valueOf(event.peerUuid));
            try {
                mm.sendDataReliable(event.peerUuid, PORT, "getInfo".getBytes());
            } catch(RightMeshException re) {}
        } else if (event.state == REMOVED){
            users.remove(event.peerUuid);
            userInfo.remove(event.peerUuid);
        }


        String output = "";
        for (MeshID user : users) {
           output += user;
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
