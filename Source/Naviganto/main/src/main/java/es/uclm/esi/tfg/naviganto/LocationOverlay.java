package es.uclm.esi.tfg.naviganto;

import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.util.GeoPoint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;

public class LocationOverlay extends Overlay {
    protected final Paint mPaint = new Paint();
    protected final Paint mAccuracyPaint = new Paint();

    protected final Bitmap POSSITION_MARKER;

    protected GeoPoint mLocation;

    private final Point screenCoords = new Point();

    private int mAccuracy = 0;
    private boolean mShowAccuracy = true;

    /* Constructors */
    public LocationOverlay(final Context ctx) {
        this(ctx, new DefaultResourceProxyImpl(ctx));
    }
    public LocationOverlay(final Context ctx, final ResourceProxy pResourceProxy) {
        super(pResourceProxy);
        this.POSSITION_MARKER = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.marker_mypossition);

        this.mAccuracyPaint.setStrokeWidth(2);
        this.mAccuracyPaint.setColor(Color.BLUE);
        this.mAccuracyPaint.setAntiAlias(true);
    }

    /* Getter & Setter */
    public void setShowAccuracy(final boolean pShowIt) {
        this.mShowAccuracy = pShowIt;
    }
    public void setLocation(final GeoPoint mp) {
        this.mLocation = mp;
    }
    public GeoPoint getLocation() {
        return this.mLocation;
    }
    public void setAccuracy(final int pAccuracy) {
        this.mAccuracy = pAccuracy;
    }

    /* Methods from SuperClass/Interfaces */
    @Override
    public void draw(final Canvas c, final MapView osmv, final boolean shadow) {
        if (shadow) {
            return;
        }

        if (this.mLocation != null) {
            final Projection pj = osmv.getProjection();
            pj.toPixels(this.mLocation, screenCoords);

            if (this.mShowAccuracy && this.mAccuracy > 10) {
                final float accuracyRadius = pj.metersToEquatorPixels(this.mAccuracy);
				/* Only draw if the DirectionArrow doesn't cover it. */
                if (accuracyRadius > 8) {
					/* Draw the inner shadow. */
                    this.mAccuracyPaint.setAntiAlias(false);
                    this.mAccuracyPaint.setAlpha(30);
                    this.mAccuracyPaint.setStyle(Style.FILL);
                    c.drawCircle(screenCoords.x, screenCoords.y, accuracyRadius, this.mAccuracyPaint);

					/* Draw the edge. */
                    this.mAccuracyPaint.setAntiAlias(true);
                    this.mAccuracyPaint.setAlpha(150);
                    this.mAccuracyPaint.setStyle(Style.STROKE);
                    c.drawCircle(screenCoords.x, screenCoords.y, accuracyRadius, this.mAccuracyPaint);
                }
            }

            c.drawBitmap(POSSITION_MARKER, screenCoords.x - this.POSSITION_MARKER.getWidth() / 2, screenCoords.y - this.POSSITION_MARKER.getHeight() / 2, this.mPaint);
        }
    }
}