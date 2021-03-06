package com.example.socketconnectionwebrtc.BootStrap;

import android.os.Bundle;

import com.example.socketconnectionwebrtc.R;
import com.example.socketconnectionwebrtc.WebRtc.PeerConnectionClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.webrtc.StatsReport;

import java.util.HashMap;
import java.util.Map;

public class fragment_hud extends Fragment {
    private TextView encoderStatView;
    private TextView hudViewBwe;
    private TextView hudViewConnection;
    private TextView hudViewVideoSend;
    private TextView hudViewVideoRecv;
    private ImageButton toggleDebugButton;
    private boolean videoCallEnabled;
    private boolean displayHud;
    private volatile boolean isRunning;


    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View controlView = inflater.inflate(R.layout.content_fragment_hud, container, false);

        // Create UI controls.
        encoderStatView = controlView.findViewById(R.id.encoder_stat_call);
        hudViewBwe = controlView.findViewById(R.id.hud_stat_bwe);
        hudViewConnection = controlView.findViewById(R.id.hud_stat_connection);
        hudViewVideoSend = controlView.findViewById(R.id.hud_stat_video_send);
        hudViewVideoRecv = controlView.findViewById(R.id.hud_stat_video_recv);
        toggleDebugButton = controlView.findViewById(R.id.button_toggle_debug);

        toggleDebugButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (displayHud) {
                    int visibility =
                            (hudViewBwe.getVisibility() == View.VISIBLE) ? View.INVISIBLE : View.VISIBLE;
                    hudViewsSetProperties(visibility);
                }
            }
        });

        return controlView;
    }

    @Override
    public void onStart() {
        super.onStart();

        Bundle args = getArguments();
        if (args != null) {
            videoCallEnabled = args.getBoolean(MainActivity.EXTRA_VIDEO_CALL, true);
            displayHud = args.getBoolean(MainActivity.EXTRA_DISPLAY_HUD, false);
        }
        int visibility = displayHud ? View.VISIBLE : View.INVISIBLE;
        encoderStatView.setVisibility(visibility);
        toggleDebugButton.setVisibility(visibility);
        hudViewsSetProperties(View.INVISIBLE);
        isRunning = true;
    }

    @Override
    public void onStop() {
        isRunning = false;
        super.onStop();
    }



    private void hudViewsSetProperties(int visibility) {
        hudViewBwe.setVisibility(visibility);
        hudViewConnection.setVisibility(visibility);
        hudViewVideoSend.setVisibility(visibility);
        hudViewVideoRecv.setVisibility(visibility);
        hudViewBwe.setTextSize(TypedValue.COMPLEX_UNIT_PT, 5);
        hudViewConnection.setTextSize(TypedValue.COMPLEX_UNIT_PT, 5);
        hudViewVideoSend.setTextSize(TypedValue.COMPLEX_UNIT_PT, 5);
        hudViewVideoRecv.setTextSize(TypedValue.COMPLEX_UNIT_PT, 5);
    }

    private Map<String, String> getReportMap(StatsReport report) {
        Map<String, String> reportMap = new HashMap<>();
        for (StatsReport.Value value : report.values) {
            reportMap.put(value.name, value.value);
        }
        return reportMap;
    }

    public void updateEncoderStatistics(final StatsReport[] reports) {
        if (!isRunning || !displayHud) {
            return;
        }
        StringBuilder encoderStat = new StringBuilder(128);
        StringBuilder bweStat = new StringBuilder();
        StringBuilder connectionStat = new StringBuilder();
        StringBuilder videoSendStat = new StringBuilder();
        StringBuilder videoRecvStat = new StringBuilder();
        String fps = null;
        String targetBitrate = null;
        String actualBitrate = null;

        for (StatsReport report : reports) {
            if (report.type.equals("ssrc") && report.id.contains("ssrc") && report.id.contains("send")) {
                // Send video statistics.
                Map<String, String> reportMap = getReportMap(report);
                String trackId = reportMap.get("googTrackId");
                if (trackId != null && trackId.contains(PeerConnectionClient.VIDEO_TRACK_ID)) {
                    fps = reportMap.get("googFrameRateSent");
                    videoSendStat.append(report.id).append("\n");
                    for (StatsReport.Value value : report.values) {
                        String name = value.name.replace("goog", "");
                        videoSendStat.append(name).append("=").append(value.value).append("\n");
                    }
                }
            } else if (report.type.equals("ssrc") && report.id.contains("ssrc")
                    && report.id.contains("recv")) {
                // Receive video statistics.
                Map<String, String> reportMap = getReportMap(report);
                // Check if this stat is for video track.
                String frameWidth = reportMap.get("googFrameWidthReceived");
                if (frameWidth != null) {
                    videoRecvStat.append(report.id).append("\n");
                    for (StatsReport.Value value : report.values) {
                        String name = value.name.replace("goog", "");
                        videoRecvStat.append(name).append("=").append(value.value).append("\n");
                    }
                }
            } else if (report.id.equals("bweforvideo")) {
                // BWE statistics.
                Map<String, String> reportMap = getReportMap(report);
                targetBitrate = reportMap.get("googTargetEncBitrate");
                actualBitrate = reportMap.get("googActualEncBitrate");

                bweStat.append(report.id).append("\n");
                for (StatsReport.Value value : report.values) {
                    String name = value.name.replace("goog", "").replace("Available", "");
                    bweStat.append(name).append("=").append(value.value).append("\n");
                }
            } else if (report.type.equals("googCandidatePair")) {
                // Connection statistics.
                Map<String, String> reportMap = getReportMap(report);
                String activeConnection = reportMap.get("googActiveConnection");
                if (activeConnection != null && activeConnection.equals("true")) {
                    connectionStat.append(report.id).append("\n");
                    for (StatsReport.Value value : report.values) {
                        String name = value.name.replace("goog", "");
                        connectionStat.append(name).append("=").append(value.value).append("\n");
                    }
                }
            }
        }
        hudViewBwe.setText(bweStat.toString());
        hudViewConnection.setText(connectionStat.toString());
        hudViewVideoSend.setText(videoSendStat.toString());
        hudViewVideoRecv.setText(videoRecvStat.toString());

        if (videoCallEnabled) {
            if (fps != null) {
                encoderStat.append("Fps:  ").append(fps).append("\n");
            }
            if (targetBitrate != null) {
                encoderStat.append("Target BR: ").append(targetBitrate).append("\n");
            }
            if (actualBitrate != null) {
                encoderStat.append("Actual BR: ").append(actualBitrate).append("\n");
            }
        }

    }
}
