package com.example.numidiapath;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MapFragment extends Fragment {

    private MapView map = null;
    private MyLocationNewOverlay mLocationOverlay;
    private CardView previewCard;
    private FirebaseFirestore db;
    private ListenerRegistration mapListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()));
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        View view = inflater.inflate(R.layout.fragment_map, container, false);

        map = view.findViewById(R.id.map);
        previewCard = view.findViewById(R.id.previewCard);

        setupMapSettings();
        setupLocationOverlay();
        setupMapClickEvents();

        view.findViewById(R.id.fabCenter).setOnClickListener(v -> recenterMap());

        loadPostsMarkers();

        return view;
    }

    private void setupMapSettings() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(8.0);
        map.getController().setCenter(new GeoPoint(36.7528, 3.0420));
    }

    private void setupLocationOverlay() {
        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), map);
        mLocationOverlay.enableMyLocation();
        map.getOverlays().add(mLocationOverlay);
    }

    private void setupMapClickEvents() {
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                hidePreview();
                return true;
            }
            @Override
            public boolean longPressHelper(GeoPoint p) { return false; }
        });
        map.getOverlays().add(0, mapEventsOverlay);
    }

    private void loadPostsMarkers() {
        mapListener = db.collection("posts").addSnapshotListener((value, error) -> {
            if (error != null) {
                if (isAdded()) Toast.makeText(getContext(), "Erreur carte: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                map.getOverlays().removeIf(overlay -> overlay instanceof Marker);

                for (DocumentSnapshot doc : value.getDocuments()) {
                    Post post = doc.toObject(Post.class);
                    if (post != null && post.getLatitude() != 0.0 && post.getLongitude() != 0.0) {
                        if (post.getPostId() == null) post.setPostId(doc.getId());
                        addPostMarker(post);
                    }
                }
                map.invalidate();
            }
        });
    }

    private void addPostMarker(Post post) {
        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(post.getLatitude(), post.getLongitude()));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(post.getLocation());

        marker.setOnMarkerClickListener((m, mapView) -> {
            showPreview(post);
            mapView.getController().animateTo(m.getPosition());
            return true;
        });

        map.getOverlays().add(marker);
    }

    private void showPreview(Post post) {
        TextView txtTitle = previewCard.findViewById(R.id.previewTitle);
        TextView txtUser = previewCard.findViewById(R.id.previewUser);
        ImageView imgPreview = previewCard.findViewById(R.id.previewImage);

        // CORRECTION ICI : On récupère en tant que View pour accepter Button ou TextView sans crash
        View btnDetails = previewCard.findViewById(R.id.btnSeeDetails);

        if (txtTitle != null) txtTitle.setText(post.getLocation());
        if (txtUser != null) txtUser.setText(String.format("Par %s", post.getUsername()));

        if (imgPreview != null && post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            Glide.with(this).load(post.getImageUrl()).centerCrop().into(imgPreview);
        }

        previewCard.setVisibility(View.VISIBLE);
        previewCard.setAlpha(0f);
        previewCard.animate().alpha(1f).setDuration(300).start();

        if (btnDetails != null) {
            btnDetails.setOnClickListener(v -> ouvrirDetailsPost(post));
        }
    }

    private void hidePreview() {
        if (previewCard != null && previewCard.getVisibility() == View.VISIBLE) {
            previewCard.animate().alpha(0f).setDuration(200).withEndAction(() ->
                    previewCard.setVisibility(View.GONE)).start();
        }
    }

    private void recenterMap() {
        if (mLocationOverlay != null && mLocationOverlay.getMyLocation() != null) {
            map.getController().animateTo(mLocationOverlay.getMyLocation());
            map.getController().setZoom(15.0);
        } else {
            Toast.makeText(getContext(), "Attente du signal GPS...", Toast.LENGTH_SHORT).show();
        }
    }

    private void ouvrirDetailsPost(Post post) {
        if (post.getPostId() == null || post.getPostId().isEmpty()) return;

        Bundle bundle = new Bundle();
        bundle.putString("postId", post.getPostId());

        PostDetailFragment detailFragment = new PostDetailFragment();
        detailFragment.setArguments(bundle);

        if (isAdded() && getActivity() != null) {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.fragment_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
        if (mLocationOverlay != null) mLocationOverlay.enableMyLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
        if (mLocationOverlay != null) mLocationOverlay.disableMyLocation();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapListener != null) mapListener.remove();
    }
}