package com.goreecloud.clips;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;

/** Consistent native outline icon controls. No font, emoji or external icon dependency. */
public final class IconButtonView extends View {
    public static final int HOME = 1, BOOKMARK = 2, DOWNLOAD = 3, SHARE = 4,
            EXPLORE = 5, LIBRARY = 6, SETTINGS = 7, OPEN = 8, TRASH = 9,
            SAVE = 10, EXTERNAL = 11, MOON = 12, SUN = 13, CHECK = 14,
            CLOSE = 15, FORWARD = 16, RETRY = 17, MORE = 18;
    private final int glyph;
    private final Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int ink;
    private int activeFill;
    private boolean highlighted;

    public IconButtonView(Context context, int glyph, String accessibilityLabel,
            int color, int fill, int outline, int activeFill, boolean highlighted, Runnable action) {
        super(context);
        this.glyph = glyph;
        this.ink = color;
        this.activeFill = activeFill;
        this.highlighted = highlighted;
        setContentDescription(accessibilityLabel);
        setTooltipText(accessibilityLabel);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setFocusable(true);
        setClickable(true);
        setSelected(highlighted);
        int corner = dip(13);
        GradientDrawable background = new GradientDrawable();
        background.setColor(highlighted ? activeFill : fill);
        background.setCornerRadius(corner);
        background.setStroke(dip(1), highlighted ? color : outline);
        setBackground(background);
        setMinimumHeight(dip(48));
        setMinimumWidth(dip(48));
        setOnClickListener(v -> action.run());
    }

    private int dip(float dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float unit = dip(24) / 24f;
        canvas.save();
        canvas.translate((getWidth() - dip(24)) / 2f, (getHeight() - dip(24)) / 2f);
        canvas.scale(unit, unit);
        pen.setColor(ink);
        pen.setStrokeWidth(1.9f);
        pen.setStrokeCap(Paint.Cap.ROUND);
        pen.setStrokeJoin(Paint.Join.ROUND);
        pen.setStyle(Paint.Style.STROKE);
        switch (glyph) {
            case HOME:
                poly(canvas, 3, 10, 12, 3, 21, 10);
                poly(canvas, 5, 9, 5, 21, 19, 21, 19, 9);
                poly(canvas, 10, 21, 10, 14, 14, 14, 14, 21);
                break;
            case BOOKMARK:
                poly(canvas, 6, 3, 18, 3, 18, 21, 12, 17, 6, 21, 6, 3);
                break;
            case DOWNLOAD:
                line(canvas, 12, 3, 12, 16);
                poly(canvas, 7, 11, 12, 16, 17, 11);
                poly(canvas, 4, 18, 4, 21, 20, 21, 20, 18);
                break;
            case SHARE:
                circle(canvas, 18, 5, 2); circle(canvas, 6, 12, 2); circle(canvas, 18, 19, 2);
                line(canvas, 8, 11, 16, 6); line(canvas, 8, 13, 16, 18);
                break;
            case EXPLORE:
                circle(canvas, 12, 12, 9);
                poly(canvas, 15.5f, 8.5f, 13.3f, 13.3f, 8.5f, 15.5f, 10.7f, 10.7f, 15.5f, 8.5f);
                break;
            case LIBRARY:
                poly(canvas, 4, 4, 9, 4, 9, 19, 4, 19, 4, 4);
                poly(canvas, 11, 3, 16, 3, 16, 19, 11, 19, 11, 3);
                line(canvas, 18, 7, 21, 19); line(canvas, 17, 14, 20, 13);
                break;
            case SETTINGS:
                circle(canvas, 12, 12, 3.2f);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * i / 4;
                    line(canvas, 12 + (float)Math.cos(a)*7, 12 + (float)Math.sin(a)*7,
                            12 + (float)Math.cos(a)*10, 12 + (float)Math.sin(a)*10);
                }
                circle(canvas, 12, 12, 7);
                break;
            case OPEN:
                poly(canvas, 14, 4, 20, 4, 20, 10);
                line(canvas, 20, 4, 10, 14);
                poly(canvas, 19, 14, 19, 20, 4, 20, 4, 5, 10, 5);
                break;
            case TRASH:
                poly(canvas, 5, 7, 19, 7);
                poly(canvas, 8, 7, 8.5f, 20, 15.5f, 20, 16, 7);
                poly(canvas, 9, 4, 15, 4);
                line(canvas, 11, 10, 11, 17);
                line(canvas, 14, 10, 14, 17);
                break;
            case SAVE:
                poly(canvas, 4, 3, 17, 3, 20, 6, 20, 21, 4, 21, 4, 3);
                poly(canvas, 7, 3, 7, 10, 16, 10, 16, 3);
                poly(canvas, 8, 21, 8, 14, 16, 14, 16, 21);
                break;
            case EXTERNAL:
                poly(canvas, 14, 4, 20, 4, 20, 10);
                line(canvas, 20, 4, 11, 13);
                poly(canvas, 19, 14, 19, 20, 4, 20, 4, 5, 10, 5);
                break;
            case MOON:
                Path moon = new Path();
                moon.moveTo(19, 16.5f); moon.cubicTo(15, 19, 8, 15, 8, 9);
                moon.cubicTo(8, 6, 9, 4, 10, 3);
                moon.cubicTo(1, 6, 2.5f, 20, 13, 21);
                moon.cubicTo(15.5f, 21, 18, 19.5f, 19, 16.5f);
                canvas.drawPath(moon, pen);
                break;
            case SUN:
                circle(canvas, 12, 12, 4);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * i / 4;
                    line(canvas, 12 + (float)Math.cos(a)*7, 12 + (float)Math.sin(a)*7,
                            12 + (float)Math.cos(a)*10, 12 + (float)Math.sin(a)*10);
                }
                break;
            case CHECK:
                poly(canvas, 4, 12, 10, 18, 20, 6); break;
            case CLOSE:
                line(canvas, 5, 5, 19, 19); line(canvas, 19, 5, 5, 19); break;
            case FORWARD:
                poly(canvas, 7, 4, 15, 12, 7, 20); break;
            case RETRY:
                canvas.drawArc(new RectF(4, 4, 20, 20), 30, 285, false, pen);
                poly(canvas, 18, 4, 20, 11, 13, 9); break;
            case MORE:
                pen.setStyle(Paint.Style.FILL);
                circle(canvas, 5, 12, 1.5f);
                circle(canvas, 12, 12, 1.5f);
                circle(canvas, 19, 12, 1.5f);
                break;
            default:
                circle(canvas, 12, 12, 7);
        }
        canvas.restore();
    }

    private void line(Canvas c, float x1, float y1, float x2, float y2) {
        c.drawLine(x1, y1, x2, y2, pen);
    }

    private void circle(Canvas c, float x, float y, float radius) {
        c.drawCircle(x, y, radius, pen);
    }

    private void poly(Canvas c, float... points) {
        if (points.length < 4 || points.length % 2 != 0) return;
        Path path = new Path();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
        c.drawPath(path, pen);
    }
}
