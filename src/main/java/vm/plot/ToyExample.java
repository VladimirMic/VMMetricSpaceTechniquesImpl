package vm.plot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import static vm.plot.AbstractPlotter.COLOURS;
import vm.plot.impl.BoxPlotPlotter;

public class ToyExample {

    public static void main(String args[]) {
        AbstractPlotter plotter = new BoxPlotPlotter();
        List<Float>[] values = getRandomValues(COLOURS.length, 2000);
        String[] tracesNames = new String[values.length];
        for (int i = 0; i < tracesNames.length; i++) {
            tracesNames[i] = "Trace " + (i + 1);
        }
        JFreeChart plot = plotter.createPlot(null, "y label", tracesNames, values);
        plotter.storePlotSVG("c:\\Data\\tmp_boxplot.svg", plot);
    }

    private static XYSeries[] prepareToyExampleTraces() {
        XYSeries[] ret = new XYSeries[]{new XYSeries("First trace"), new XYSeries("Second trace")};
        Random r = new Random();
        for (int i = 0; i < 10; i++) {
            float x = r.nextFloat(0.76f);
            float y = r.nextFloat(0.9f);
            System.out.println(x + ", " + y);
            ret[0].add(x, y);
            ret[1].add(x / 2f, y / 2f);
        }
        return ret;
    }

    private static List<Float>[] getRandomValues(int boxplotsCount, int valuesCount) {
        Random r = new Random();
        List<Float>[] ret = new List[boxplotsCount];
        for (int i = 0; i < boxplotsCount; i++) {
            ret[i] = new ArrayList<>();
            for (int j = 0; j < valuesCount; j++) {
                ret[i].add((float) Math.pow(r.nextFloat(), i + 1));
            }
        }
        return ret;
    }

}
