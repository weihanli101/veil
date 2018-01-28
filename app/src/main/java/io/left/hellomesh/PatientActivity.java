package io.left.hellomesh;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashMap;
import java.util.HashSet;
import io.left.rightmesh.android.AndroidMeshManager;
import io.left.rightmesh.android.MeshService;
import io.left.rightmesh.id.MeshID;
import io.left.rightmesh.mesh.MeshManager;
import io.left.rightmesh.mesh.MeshStateListener;
import io.left.rightmesh.util.RightMeshException;
import io.reactivex.functions.Consumer;

import static io.left.rightmesh.mesh.MeshManager.DATA_RECEIVED;
import static io.left.rightmesh.mesh.MeshManager.PEER_CHANGED;
import static io.left.rightmesh.mesh.MeshManager.REMOVED;

public class PatientActivity extends AppCompatActivity implements MeshStateListener{
    private class UserInfo {
        String name;
        String role;

        public UserInfo (String _name, String _role){
            name = _name;
            role = _role;
        }
    }

    private static final int PORT = 9876;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    // MeshManager instance - interface to the mesh network.
    AndroidMeshManager mm = null;
    // Set to keep track of peers connected to the mesh.
    HashSet<MeshID> users = new HashSet<>();
    HashMap<MeshID, UserInfo> userInfo = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);


        mm = AndroidMeshManager.getInstance(PatientActivity.this, PatientActivity.this);
    }

    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();
            //shuts down Android Mesh Manager
            mm.stop();
        } catch (MeshService.ServiceDisconnectedException e) {
            e.printStackTrace();
        }
    }

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

        if (args[0].equals("receiveInfo")) {
            String name = args[1];
            String role = args[2];

            if (role.equals("paramedic")) {
                userInfo.put(event.peerUuid, new PatientActivity.UserInfo(name, role));

                TextView patientList = (TextView) findViewById(R.id.user_profile_name);
                patientList.append("Peer Id: " + peerUuid + "\n" + data + "\n\n");
            }
        } else if (args[0].equals("getInfo")) {
            try {
                mm.sendDataReliable(event.peerUuid, PORT, "receiveInfo;andy;patient;".getBytes());
            } catch (RightMeshException re) {}
        }
    }


    private void handlePeerChanged(MeshManager.RightMeshEvent e) {
        // Update peer list.
        MeshManager.PeerChangedEvent event = (MeshManager.PeerChangedEvent) e;
        if (event.state != REMOVED && !users.contains(event.peerUuid)) {
            users.add(event.peerUuid);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_patient, menu);
        return true;
    }

    public static class PlaceholderFragment extends Fragment {
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_patient, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
    }

}
