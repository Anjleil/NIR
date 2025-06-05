package project.NIR.Utils;

import org.jxmapviewer.viewer.GeoPosition;

public class GeoUtils {

    private static final double EARTH_RADIUS_METERS = 6371000;

    /**
     * Calculates the distance between two GeoPositions using the Haversine formula.
     * @param p1 First GeoPosition
     * @param p2 Second GeoPosition
     * @return Distance in meters
     */
    public static double calculateDistance(GeoPosition p1, GeoPosition p2) {
        if (p1 == null || p2 == null) {
            return 0.0;
        }

        double lat1Rad = Math.toRadians(p1.getLatitude());
        double lon1Rad = Math.toRadians(p1.getLongitude());
        double lat2Rad = Math.toRadians(p2.getLatitude());
        double lon2Rad = Math.toRadians(p2.getLongitude());

        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Calculates an intermediate GeoPosition along the great-circle path between two points.
     * @param startPoint Starting GeoPosition
     * @param endPoint Ending GeoPosition
     * @param fraction The fraction of the distance from startPoint to endPoint (0.0 to 1.0)
     * @return Intermediate GeoPosition
     */
    public static GeoPosition calculateIntermediatePoint(GeoPosition startPoint, GeoPosition endPoint, double fraction) {
        if (startPoint == null || endPoint == null) {
            return null; // Or throw an IllegalArgumentException
        }
        if (fraction <= 0.0) return startPoint;
        if (fraction >= 1.0) return endPoint;

        double lat1 = Math.toRadians(startPoint.getLatitude());
        double lon1 = Math.toRadians(startPoint.getLongitude());
        double lat2 = Math.toRadians(endPoint.getLatitude());
        double lon2 = Math.toRadians(endPoint.getLongitude());

        // Calculate angular distance d
        double d = 2 * Math.asin(Math.sqrt(
                Math.sin((lat2 - lat1) / 2) * Math.sin((lat2 - lat1) / 2) +
                Math.cos(lat1) * Math.cos(lat2) * 
                Math.sin((lon2 - lon1) / 2) * Math.sin((lon2 - lon1) / 2)));
        
        // if d is very small, points are basically the same
        if (d == 0) return startPoint; 

        double A = Math.sin((1 - fraction) * d) / Math.sin(d);
        double B = Math.sin(fraction * d) / Math.sin(d);

        double x = A * Math.cos(lat1) * Math.cos(lon1) + B * Math.cos(lat2) * Math.cos(lon2);
        double y = A * Math.cos(lat1) * Math.sin(lon1) + B * Math.cos(lat2) * Math.sin(lon2);
        double z = A * Math.sin(lat1) + B * Math.sin(lat2);

        double latIntermediate = Math.atan2(z, Math.sqrt(x * x + y * y));
        double lonIntermediate = Math.atan2(y, x);

        return new GeoPosition(Math.toDegrees(latIntermediate), Math.toDegrees(lonIntermediate));
    }

    /**
     * Calculates the initial bearing (forward azimuth) from one point to another.
     * @param p1 the start point
     * @param p2 the end point
     * @return The bearing in radians
     */
    public static double calculateBearing(GeoPosition p1, GeoPosition p2) {
        if (p1 == null || p2 == null) {
            return 0.0;
        }

        double lat1 = Math.toRadians(p1.getLatitude());
        double lon1 = Math.toRadians(p1.getLongitude());
        double lat2 = Math.toRadians(p2.getLatitude());
        double lon2 = Math.toRadians(p2.getLongitude());

        double dLon = lon2 - lon1;

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

        return Math.atan2(y, x);
    }
} 