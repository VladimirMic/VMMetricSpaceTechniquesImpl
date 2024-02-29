package vm.plot;

import java.util.Random;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;

public class ToyExample {

    public static void main(String args[]) {
        XYSeries[] traces = prepareToyExampleTraces();
        AbstractPlotter plotter = new XYLinesPlotter();
        JFreeChart plot = plotter.createPlot(null, "x label", "y label", null, null, traces);
        plotter.storePlot("c:\\Data\\tmp.png", plot);
    }

    private static XYSeries[] prepareToyExampleTraces() {
        XYSeries[] ret = new XYSeries[]{new XYSeries("First trace"), new XYSeries("Second trace")};
        Random r = new Random();
        for (int i = 0; i < 10; i++) {
            float x = r.nextFloat(1000);
            float y = r.nextFloat(100000);
            System.out.println(x + ", " + y);
            ret[0].add(x, y);
            ret[1].add(x / 2f, y / 2f);
        }
        return ret;
    }

}
