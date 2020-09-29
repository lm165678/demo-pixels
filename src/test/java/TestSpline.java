import com.jayfella.pixels.core.SplineInterpolator;

import java.util.ArrayList;
import java.util.List;

public class TestSpline {


    public static void main(String... args) {

        List<Float> xAxis = new ArrayList<>();
        xAxis.add(0f);
        xAxis.add(0.5f);
        xAxis.add(1f);

        List<Float> yAxis = new ArrayList<>();
        yAxis.add(0f);
        yAxis.add(0.75f);
        yAxis.add(1f);

        SplineInterpolator interpolator = SplineInterpolator.createMonotoneCubicSpline(xAxis, yAxis);
        float val = interpolator.interpolate(0.53f);

        System.out.print("Value: " + val);

    }

}
