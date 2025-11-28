package org.example.service;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.style.Styler;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChartService {

    public byte[] renderTimeSeriesChart(Map<String, Integer> timeSeries, String title, String xLabel, String yLabel) throws Exception {
        if (timeSeries == null || timeSeries.isEmpty()) {
            timeSeries = new java.util.LinkedHashMap<>();
            timeSeries.put(java.time.LocalDate.now().toString(), 0);
        }
        java.util.List<String> keys = new java.util.ArrayList<>(timeSeries.keySet());
        java.util.List<Integer> values = new java.util.ArrayList<>(keys.size());
        for (String k : keys) {
            Integer v = timeSeries.get(k);
            values.add(v == null ? 0 : v);
        }
        CategoryChart chart = new CategoryChartBuilder().width(1000).height(400)
                .title(title == null ? "Recognitions" : title)
                .xAxisTitle(xLabel == null ? "time" : xLabel)
                .yAxisTitle(yLabel == null ? "count" : yLabel)
                .build();
        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setXAxisLabelRotation(45);
        chart.addSeries("count", keys, values);
        java.awt.image.BufferedImage img = BitmapEncoder.getBufferedImage(chart);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }
}
