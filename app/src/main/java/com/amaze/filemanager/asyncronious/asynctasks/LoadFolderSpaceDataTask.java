package com.amaze.filemanager.asyncronious.asynctasks;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.text.SpannableString;
import android.text.format.Formatter;
import android.view.View;

import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.utils.files.FileUtils;
import com.amaze.filemanager.utils.OnProgressUpdate;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.List;

import static com.amaze.filemanager.utils.Utils.getColor;

/**
 * Loads data for chart in FileUtils.showPropertiesDialog()
 *
 * @author Emmanuel Messulam<emmanuelbendavid@gmail.com>
 *         on 12/5/2017, at 00:07.
 */

public class LoadFolderSpaceDataTask extends AsyncTask<Void, Long, Pair<String, List<PieEntry>>> {

    private static int[] COLORS;
    private static String[] LEGENDS;

    private Context context;
    private AppTheme appTheme;
    private PieChart chart;
    private BaseFile file;

    public LoadFolderSpaceDataTask(Context c, AppTheme appTheme, PieChart chart, BaseFile f) {
        context = c;
        this.appTheme = appTheme;
        this.chart = chart;
        file = f;
        LEGENDS = new String[]{context.getString(R.string.size), context.getString(R.string.used_by_others), context.getString(R.string.free)};
        COLORS = new int[]{getColor(c, R.color.piechart_red), getColor(c, R.color.piechart_blue),
                getColor(c, R.color.piechart_green)};
    }

    @Override
    protected Pair<String, List<PieEntry>> doInBackground(Void... params) {
        long[] dataArray = FileUtils.getSpaces(file, context, new OnProgressUpdate<Long[]>() {
            @Override
            public void onUpdate(Long[] data) {
                publishProgress(data);
            }
        });

        if (dataArray[0] != -1 && dataArray[0] != 0) {
            long totalSpace = dataArray[0];

            List<PieEntry> entries = createEntriesFromArray(dataArray, false);

            return new Pair<>(Formatter.formatFileSize(context, totalSpace), entries);
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Long[] dataArray) {
        if (dataArray[0] != -1 && dataArray[0] != 0) {
            long totalSpace = dataArray[0];

            List<PieEntry> entries = createEntriesFromArray(
                    new long[]{dataArray[0], dataArray[1], dataArray[2]},
                    true);

            updateChart(Formatter.formatFileSize(context, totalSpace), entries);

            chart.notifyDataSetChanged();
            chart.invalidate();
        }
    }

    @Override
    protected void onPostExecute(Pair<String, List<PieEntry>> data) {
        if(data == null) {
            chart.setVisibility(View.GONE);
            return;
        }

        updateChart(data.first, data.second);

        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private List<PieEntry> createEntriesFromArray(long[] dataArray, boolean loading) {
        long usedByFolder = dataArray[2],
                usedByOther = dataArray[0] - dataArray[1] - dataArray[2],
                freeSpace = dataArray[1];

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(usedByFolder, LEGENDS[0], loading? ">":null));
        entries.add(new PieEntry(usedByOther, LEGENDS[1], loading? "<":null));
        entries.add(new PieEntry(freeSpace, LEGENDS[2]));

        return entries;
    }

    private void updateChart(String totalSpace, List<PieEntry> entries) {
        boolean isDarkTheme = appTheme.getMaterialDialogTheme() == Theme.DARK;

        PieDataSet set = new PieDataSet(entries, null);
        set.setColors(COLORS);
        set.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        set.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        set.setSliceSpace(5f);
        set.setAutomaticallyDisableSliceSpacing(true);
        set.setValueLinePart2Length(1.05f);
        set.setSelectionShift(0f);

        PieData pieData = new PieData(set);
        pieData.setValueFormatter(new GeneralDialogCreation.SizeFormatter(context));
        pieData.setValueTextColor(isDarkTheme? Color.WHITE:Color.BLACK);

        chart.setCenterText(new SpannableString(context.getString(R.string.total) + "\n" + totalSpace));
        chart.setData(pieData);
    }

}
