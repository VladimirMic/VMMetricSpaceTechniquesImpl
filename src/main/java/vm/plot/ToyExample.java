package vm.plot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import vm.plot.impl.BoxPlotCategoricalPlotter;

public class ToyExample {

    public static void main(String args[]) {
        AbstractPlotter plotter = new BoxPlotCategoricalPlotter();
        int groupsCount = 3;
        int tracesCount = 6;
        List<Float>[][] values = getRandomValues(groupsCount, tracesCount, 2000);
        String[] tracesNames = new String[tracesCount];
        String[] groupsNames = new String[groupsCount];
        for (int i = 0; i < groupsCount; i++) {
            groupsNames[i] = "Group " + (i + 1);
        }
        for (int i = 0; i < tracesNames.length; i++) {
            tracesNames[i] = "Trace " + (i + 1);
        }
        JFreeChart plot = plotter.createPlot(null, "y label", tracesNames, groupsNames, values);
        plotter.storePlotPNG("c:\\Data\\tmp_boxplot.svg", plot);
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

    private static List<Float>[][] getRandomValues(int groupsCount, int boxplotsCount, int valuesCount) {
        Random r = new Random();
        List<Float>[][] ret = new List[groupsCount][boxplotsCount];
        for (int i = 0; i < boxplotsCount; i++) {
            for (int j = 0; j < groupsCount; j++) {
                ret[j][i] = new ArrayList<>();
                for (int k = 0; k < valuesCount; k++) {
                    ret[j][i].add((float) Math.pow(r.nextFloat(), i + 1));
                }
            }
        }
        return ret;
    }

}
