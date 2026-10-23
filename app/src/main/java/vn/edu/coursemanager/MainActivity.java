package vn.edu.coursemanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native Android screens for browsing and maintaining courses backed by MockAPI. */
public class MainActivity extends Activity {
    private static final int INK = Color.rgb(24, 35, 58);
    private static final int MUTED = Color.rgb(111, 125, 149);
    private static final int BLUE = Color.rgb(48, 97, 232);
    private static final int CANVAS = Color.rgb(246, 248, 252);
    private static final int LINE = Color.rgb(231, 235, 243);
    private final ExecutorService io = Executors.newFixedThreadPool(3);
    private final List<Course> courses = new ArrayList<>();
    private final List<Category> categories = new ArrayList<>();
    private String baseUrl, screen = "courses", search = "", categoryFilter = "";
    private Course selectedCourse;
    private LinearLayout root, body, header, bottom;
    private boolean loading = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(CANVAS);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        baseUrl = getPreferences(MODE_PRIVATE).getString("base_url", "");
        render();
        if (baseUrl.isEmpty()) showSettings(true);
        else loadData();
    }

    private void render() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(CANVAS);
        setContentView(root);

        header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(14), dp(18), dp(12));
        header.setBackgroundColor(CANVAS);
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(66)));
        TextView brand = text("STUDY\nSPACE", 10, BLUE, true);
        brand.setLetterSpacing(.12f);
        header.addView(brand, new LinearLayout.LayoutParams(dp(66), -2));
        View divider = new View(this);
        divider.setBackgroundColor(LINE);
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(dp(1), dp(32));
        dl.setMargins(dp(9), 0, dp(13), 0);
        header.addView(divider, dl);
        LinearLayout brandText = column();
        TextView appName = text("HỌC TẬP", 10, MUTED, true);
        appName.setLetterSpacing(.14f);
        brandText.addView(appName);
        TextView title = text(screen.equals("detail") ? "Chi tiết" : screen.equals("categories") ? "Danh mục" : "Khóa học", 17, INK, true);
        brandText.addView(title);
        header.addView(brandText, new LinearLayout.LayoutParams(0, -2, 1));
        TextView settings = text("⚙", 22, INK, true);
        settings.setGravity(Gravity.CENTER);
        settings.setBackground(round(Color.WHITE, 14));
        header.addView(settings, new LinearLayout.LayoutParams(dp(44), dp(44)));
        settings.setOnClickListener(v -> showSettings(false));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        body = column();
        body.setPadding(dp(20), dp(12), dp(20), dp(28));
        scroll.addView(body);

        bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER);
        bottom.setBackgroundColor(Color.WHITE);
        bottom.setPadding(dp(10), dp(8), dp(10), dp(8));
        root.addView(bottom, new LinearLayout.LayoutParams(-1, dp(68)));
        addNav("▦", "Khóa học", "courses");
        addNav("◈", "Danh mục", "categories");
        addNav("＋", "Thêm mới", "new");

        if (screen.equals("detail") && selectedCourse != null) renderDetail();
        else if (screen.equals("categories")) renderCategories();
        else renderCourses();
    }

    private void addNav(String icon, String label, String target) {
        LinearLayout item = column();
        item.setGravity(Gravity.CENTER);
        boolean active = screen.equals(target) || (target.equals("courses") && screen.equals("detail"));
        TextView glyph = text(icon, 20, active ? BLUE : MUTED, true);
        glyph.setGravity(Gravity.CENTER);
        item.addView(glyph);
        TextView caption = text(label, 10, active ? BLUE : MUTED, active);
        item.addView(caption);
        bottom.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
        item.setOnClickListener(v -> {
            if (target.equals("new")) { if (categories.isEmpty()) toast("Hãy tải danh mục trước khi thêm khóa học."); else showCourseForm(null); }
            else { screen = target; selectedCourse = null; render(); }
        });
    }

    private void renderCourses() {
        TextView eyebrow = text("KHÔNG GIAN HỌC TẬP CỦA BẠN", 10, BLUE, true);
        eyebrow.setLetterSpacing(.1f);
        body.addView(eyebrow);
        TextView headline = text("Tìm khóa học\ntiếp theo", 29, INK, true);
        headline.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(-1, -2);
        hl.topMargin = dp(9);
        body.addView(headline, hl);
        TextView subtitle = text("Kiến thức mới, mở ra cơ hội mới.", 13, MUTED, false);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(-1, -2);
        sl.topMargin = dp(7);
        body.addView(subtitle, sl);

        LinearLayout summary = row();
        summary.setPadding(dp(17), dp(15), dp(17), dp(15));
        summary.setBackground(round(Color.rgb(31, 53, 103), 18));
        LinearLayout summaryText = column();
        summaryText.addView(text("KHÓA HỌC ĐANG CÓ", 10, Color.rgb(185, 203, 245), true));
        summaryText.addView(text(String.valueOf(courses.size()), 27, Color.WHITE, true));
        summary.addView(summaryText, new LinearLayout.LayoutParams(0, -2, 1));
        TextView miniIcon = text("✦", 24, Color.WHITE, true);
        miniIcon.setGravity(Gravity.CENTER);
        miniIcon.setBackground(round(Color.rgb(56, 79, 139), 16));
        summary.addView(miniIcon, new LinearLayout.LayoutParams(dp(47), dp(47)));
        LinearLayout.LayoutParams sumlp = new LinearLayout.LayoutParams(-1, -2);
        sumlp.topMargin = dp(21);
        body.addView(summary, sumlp);

        LinearLayout searchRow = row();
        EditText searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setTextSize(14);
        searchBox.setHint("Tìm tên khóa học, giảng viên...");
        searchBox.setHintTextColor(MUTED);
        searchBox.setTextColor(INK);
        searchBox.setPadding(dp(14), 0, dp(10), 0);
        searchBox.setCompoundDrawablePadding(dp(9));
        searchBox.setBackground(round(Color.WHITE, 14));
        searchBox.setText(search);
        searchRow.addView(searchBox, new LinearLayout.LayoutParams(0, dp(49), 1));
        TextView refresh = text(loading ? "…" : "↻", 22, BLUE, true);
        refresh.setGravity(Gravity.CENTER);
        refresh.setBackground(round(Color.WHITE, 14));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(dp(49), dp(49));
        rlp.leftMargin = dp(9);
        searchRow.addView(refresh, rlp);
        refresh.setOnClickListener(v -> loadData());
        LinearLayout.LayoutParams searchlp = new LinearLayout.LayoutParams(-1, -2);
        searchlp.topMargin = dp(16);
        body.addView(searchRow, searchlp);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) { search = s.toString(); renderCourseCards(); }
            @Override public void afterTextChanged(Editable e) { }
        });

        HorizontalScroller chips = new HorizontalScroller(this);
        chips.setPadding(0, dp(12), 0, dp(2));
        chips.addChip("Tất cả", "", categoryFilter.isEmpty());
        for (Category c : categories) chips.addChip(c.name, c.id, c.id.equals(categoryFilter));
        LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(-1, dp(51));
        body.addView(chips, chipLp);

        TextView section = text("Khóa học nổi bật", 17, INK, true);
        LinearLayout.LayoutParams secLp = new LinearLayout.LayoutParams(-1, -2);
        secLp.topMargin = dp(15);
        body.addView(section, secLp);
        LinearLayout cards = column();
        cards.setTag("course_cards");
        body.addView(cards);
        renderCourseCards();
        if (loading) addStatus("Đang tải dữ liệu từ MockAPI…", false);
        else if (baseUrl.isEmpty()) addStatus("Nhập URL MockAPI để bắt đầu. Chạm biểu tượng ⚙ phía trên.", true);
    }

    private void renderCourseCards() {
        if (body == null) return;
        View view = body.findViewWithTag("course_cards");
        if (!(view instanceof LinearLayout)) return;
        LinearLayout cards = (LinearLayout) view;
        cards.removeAllViews();
        List<Course> filtered = new ArrayList<>();
        for (Course c : courses) {
            boolean matchText = (c.title + " " + c.instructor).toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
            if (matchText && (categoryFilter.isEmpty() || categoryFilter.equals(c.categoryId))) filtered.add(c);
        }
        if (filtered.isEmpty()) {
            LinearLayout empty = column();
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(24), dp(27), dp(24), dp(27));
            empty.setBackground(round(Color.WHITE, 16));
            empty.addView(text(courses.isEmpty() ? "Chưa có khóa học" : "Không tìm thấy kết quả", 15, INK, true));
            TextView hint = text(courses.isEmpty() ? "Dữ liệu sẽ xuất hiện sau khi bạn kết nối MockAPI." : "Thử từ khóa hoặc danh mục khác nhé.", 12, MUTED, false);
            LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-2, -2);
            hp.topMargin = dp(6);
            empty.addView(hint, hp);
            cards.addView(empty);
            return;
        }
        for (Course c : filtered) cards.addView(courseCard(c));
    }

    private View courseCard(Course course) {
        LinearLayout card = column();
        card.setBackground(round(Color.WHITE, 17));
        card.setClipToOutline(true);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.topMargin = dp(12);
        card.setLayoutParams(cp);
        FrameLayout cover = new FrameLayout(this);
        cover.setBackground(gradient(courseColor(course.id), Color.rgb(71, 122, 219), 16));
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cover.addView(image, new FrameLayout.LayoutParams(-1, dp(156)));
        TextView badge = text("LEARN  ·  GROW", 10, Color.WHITE, true);
        badge.setLetterSpacing(.11f);
        badge.setPadding(dp(11), dp(7), dp(11), dp(7));
        badge.setBackground(round(Color.argb(75, 10, 25, 60), 10));
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.LEFT);
        bp.setMargins(dp(13), 0, 0, dp(13));
        cover.addView(badge, bp);
        card.addView(cover, new LinearLayout.LayoutParams(-1, dp(156)));
        loadImage(course.thumbnail, image);

        LinearLayout info = column();
        info.setPadding(dp(15), dp(13), dp(15), dp(14));
        TextView cat = text(categoryName(course.categoryId).toUpperCase(Locale.ROOT), 9, BLUE, true);
        cat.setLetterSpacing(.08f);
        info.addView(cat);
        TextView name = text(course.title, 17, INK, true);
        name.setMaxLines(2);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(5);
        info.addView(name, np);
        LinearLayout teacherRow = row();
        teacherRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView teacher = text("◉  " + (course.instructor.isEmpty() ? "Chưa cập nhật giảng viên" : course.instructor), 12, MUTED, false);
        teacherRow.addView(teacher, new LinearLayout.LayoutParams(0, -2, 1));
        String price = formatPrice(course.price);
        TextView cost = text(price, 12, BLUE, true);
        teacherRow.addView(cost);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.topMargin = dp(10);
        info.addView(teacherRow, tp);
        card.addView(info);
        card.setOnClickListener(v -> { selectedCourse = course; screen = "detail"; render(); });
        return card;
    }

    private void renderDetail() {
        Course c = selectedCourse;
        TextView back = text("‹   Tất cả khóa học", 13, BLUE, true);
        body.addView(back);
        back.setOnClickListener(v -> { screen = "courses"; render(); });
        FrameLayout cover = new FrameLayout(this);
        cover.setBackground(gradient(courseColor(c.id), Color.rgb(78, 125, 218), 19));
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cover.addView(image, new FrameLayout.LayoutParams(-1, -1));
        body.addView(cover, new LinearLayout.LayoutParams(-1, dp(205)));
        LinearLayout.LayoutParams cvp = (LinearLayout.LayoutParams) cover.getLayoutParams();
        cvp.topMargin = dp(14);
        cover.setLayoutParams(cvp);
        loadImage(c.thumbnail, image);

        TextView cat = text(categoryName(c.categoryId).toUpperCase(Locale.ROOT), 10, BLUE, true);
        cat.setLetterSpacing(.1f);
        LinearLayout.LayoutParams catp = new LinearLayout.LayoutParams(-1, -2);
        catp.topMargin = dp(18);
        body.addView(cat, catp);
        TextView title = text(c.title, 27, INK, true);
        LinearLayout.LayoutParams titlep = new LinearLayout.LayoutParams(-1, -2);
        titlep.topMargin = dp(6);
        body.addView(title, titlep);
        TextView meta = text("◉  " + c.instructor + "     ·     ◷  " + c.duration + " giờ", 13, MUTED, false);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, -2);
        mp.topMargin = dp(12);
        body.addView(meta, mp);

        LinearLayout actions = row();
        Button edit = actionButton("Chỉnh sửa", false);
        Button delete = actionButton("Xóa khóa học", true);
        actions.addView(edit, new LinearLayout.LayoutParams(0, dp(46), 1));
        LinearLayout.LayoutParams delp = new LinearLayout.LayoutParams(0, dp(46), 1);
        delp.leftMargin = dp(9);
        actions.addView(delete, delp);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
        ap.topMargin = dp(17);
        body.addView(actions, ap);
        edit.setOnClickListener(v -> showCourseForm(c));
        delete.setOnClickListener(v -> confirmDeleteCourse(c));

        TextView descHead = text("Về khóa học", 17, INK, true);
        LinearLayout.LayoutParams dhp = new LinearLayout.LayoutParams(-1, -2);
        dhp.topMargin = dp(22);
        body.addView(descHead, dhp);
        TextView desc = text(c.description.isEmpty() ? "Chưa có mô tả cho khóa học này." : c.description, 14, MUTED, false);
        desc.setLineSpacing(dp(4), 1f);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2);
        dp.topMargin = dp(8);
        body.addView(desc, dp);

        LinearLayout priceBox = row();
        priceBox.setGravity(Gravity.CENTER_VERTICAL);
        priceBox.setPadding(dp(16), dp(14), dp(16), dp(14));
        priceBox.setBackground(round(Color.WHITE, 16));
        LinearLayout priceText = column();
        priceText.addView(text("HỌC PHÍ", 10, MUTED, true));
        priceText.addView(text(formatPrice(c.price), 20, BLUE, true));
        priceBox.addView(priceText, new LinearLayout.LayoutParams(0, -2, 1));
        body.addView(priceBox, layout(-1, -2, 0, 16, 0, 0, 0, 0));

        TextView trailerTitle = text("Trailer khóa học", 17, INK, true);
        body.addView(trailerTitle, layout(-1, -2, 0, 20, 0, 0, 0, 0));
        if (c.trailerUrl.trim().isEmpty()) {
            TextView noTrailer = text("Khóa học chưa có đường dẫn trailer.", 13, MUTED, false);
            body.addView(noTrailer, layout(-1, -2, 0, 7, 0, 0, 0, 0));
        } else {
            VideoView video = new VideoView(this);
            video.setBackground(gradient(Color.rgb(24, 35, 58), Color.rgb(53, 72, 107), 14));
            body.addView(video, layout(-1, dp(205), 0, 9, 0, 0, 0, 0));
            Button play = actionButton("▶  Phát trailer", false);
            body.addView(play, layout(-1, dp(47), 0, 8, 0, 0, 0, 0));
            play.setOnClickListener(v -> {
                try {
                    video.setVideoURI(Uri.parse(c.trailerUrl.trim()));
                    video.setOnPreparedListener(player -> { player.setLooping(false); video.start(); });
                    video.setOnErrorListener((player, what, extra) -> { toast("Không phát được URL này. Hãy kiểm tra liên kết video."); return true; });
                } catch (Exception e) { toast("URL trailer không hợp lệ."); }
            });
        }
    }

    private void renderCategories() {
        body.addView(text("DANH MỤC HỌC TẬP", 10, BLUE, true));
        body.addView(text("Khám phá theo\nchủ đề", 28, INK, true), layout(-1, -2, 0, 8, 0, 0, 0, 0));
        body.addView(text("Các chủ đề được tải trực tiếp từ MockAPI.", 13, MUTED, false), layout(-1, -2, 0, 6, 0, 0, 0, 0));
        Button add = actionButton("＋   Thêm danh mục", false);
        body.addView(add, layout(-1, dp(47), 0, 17, 0, 0, 0, 0));
        add.setOnClickListener(v -> showCategoryForm(null));
        if (loading) addStatus("Đang tải danh mục…", false);
        if (categories.isEmpty() && !loading) addStatus(baseUrl.isEmpty() ? "Thiết lập URL MockAPI để xem danh mục." : "Chưa có danh mục. Hãy tạo danh mục đầu tiên.", true);
        for (Category category : categories) {
            LinearLayout card = row();
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(15), dp(13), dp(12), dp(13));
            card.setBackground(round(Color.WHITE, 16));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.topMargin = dp(11);
            body.addView(card, cp);
            TextView icon = text("◈", 20, BLUE, true);
            icon.setGravity(Gravity.CENTER);
            icon.setBackground(round(Color.rgb(237, 242, 255), 14));
            card.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));
            LinearLayout words = column();
            words.setPadding(dp(12), 0, dp(8), 0);
            words.addView(text(category.name, 15, INK, true));
            words.addView(text(category.description.isEmpty() ? "Chưa có mô tả" : category.description, 12, MUTED, false));
            card.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
            TextView more = text("···", 21, MUTED, true);
            more.setGravity(Gravity.CENTER);
            card.addView(more, new LinearLayout.LayoutParams(dp(35), dp(42)));
            more.setOnClickListener(v -> showCategoryActions(category));
            card.setOnClickListener(v -> { screen = "courses"; categoryFilter = category.id; render(); });
        }
    }

    private void showSettings(boolean firstRun) {
        LinearLayout form = column();
        form.setPadding(dp(22), dp(8), dp(22), dp(2));
        form.addView(text("Kết nối dữ liệu", 21, INK, true));
        TextView instruction = text("Dán Base URL của project MockAPI.\nVí dụ: https://YOUR_PROJECT_ID.mockapi.io/api/v1", 13, MUTED, false);
        instruction.setLineSpacing(dp(3), 1f);
        form.addView(instruction, layout(-1, -2, 0, 7, 0, 13, 0, 0));
        EditText url = field("https://your-project.mockapi.io/api/v1", false);
        url.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        url.setText(baseUrl);
        form.addView(url, layout(-1, dp(49), 0, 4, 0, 0, 0, 0));
        AlertDialog dialog = new AlertDialog.Builder(this).setView(form)
                .setNegativeButton(firstRun ? "Để sau" : "Đóng", (d, w) -> { if (firstRun && baseUrl.isEmpty()) render(); })
                .setPositiveButton("Lưu và tải", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String candidate = url.getText().toString().trim().replaceAll("/+$", "");
            if (!candidate.startsWith("https://") || candidate.contains("YOUR_PROJECT_ID")) {
                url.setError("Nhập Base URL MockAPI của project bạn."); return;
            }
            baseUrl = candidate;
            getPreferences(MODE_PRIVATE).edit().putString("base_url", baseUrl).apply();
            dialog.dismiss();
            screen = "courses";
            loadData();
        }));
        dialog.show();
    }

    private void showCourseForm(Course original) {
        if (categories.isEmpty()) { toast("Bạn cần tạo ít nhất một danh mục trước."); return; }
        LinearLayout form = column();
        form.setPadding(dp(20), dp(4), dp(20), dp(4));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);
        EditText title = addField(form, "Tên khóa học *", original == null ? "" : original.title, false);
        EditText description = addField(form, "Mô tả", original == null ? "" : original.description, true);
        String[] names = new String[categories.size()];
        int chosen = 0;
        for (int i = 0; i < categories.size(); i++) { names[i] = categories.get(i).name; if (original != null && categories.get(i).id.equals(original.categoryId)) chosen = i; }
        TextView categoryLabel = text("Danh mục *", 12, MUTED, true);
        form.addView(categoryLabel, layout(-1, -2, 0, 8, 0, 6, 0, 0));
        Spinner category = new Spinner(this);
        category.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        category.setSelection(chosen);
        form.addView(category, layout(-1, dp(48), 0, 0, 0, 0, 0, 4));
        EditText instructor = addField(form, "Giảng viên *", original == null ? "" : original.instructor, false);
        EditText price = addField(form, "Học phí (VNĐ) *", original == null ? "" : numberValue(original.price), false);
        price.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText duration = addField(form, "Thời lượng (giờ) *", original == null ? "" : String.valueOf(original.duration), false);
        duration.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        EditText thumbnail = addField(form, "URL ảnh bìa", original == null ? "" : original.thumbnail, false);
        thumbnail.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        EditText trailer = addField(form, "URL trailer video", original == null ? "" : original.trailerUrl, false);
        trailer.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(original == null ? "Thêm khóa học" : "Chỉnh sửa khóa học")
                .setView(scroll).setNegativeButton("Hủy", null).setPositiveButton(original == null ? "Tạo khóa học" : "Lưu thay đổi", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String t = title.getText().toString().trim(), teach = instructor.getText().toString().trim();
            double p; int hours;
            try { p = Double.parseDouble(price.getText().toString().trim().replace(",", "")); }
            catch (Exception ex) { price.setError("Nhập học phí hợp lệ."); return; }
            try { hours = Integer.parseInt(duration.getText().toString().trim()); }
            catch (Exception ex) { duration.setError("Nhập số giờ hợp lệ."); return; }
            if (t.isEmpty()) { title.setError("Vui lòng nhập tên khóa học."); return; }
            if (teach.isEmpty()) { instructor.setError("Vui lòng nhập tên giảng viên."); return; }
            if (p < 0) { price.setError("Học phí không được âm."); return; }
            if (hours < 0) { duration.setError("Thời lượng không được âm."); return; }
            Course c = original == null ? new Course() : original;
            c.title = t; c.description = description.getText().toString(); c.categoryId = categories.get(category.getSelectedItemPosition()).id;
            c.instructor = teach; c.price = p; c.duration = hours; c.thumbnail = thumbnail.getText().toString(); c.trailerUrl = trailer.getText().toString();
            if (original == null) c.createdAt = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            dialog.dismiss(); hideKeyboard();
            mutate(original == null ? "Đang tạo khóa học…" : "Đang lưu thay đổi…", api -> { if (original == null) api.createCourse(c); else api.updateCourse(c); }, "Lưu khóa học thành công.");
        }));
        dialog.show();
    }

    private void showCategoryForm(Category original) {
        LinearLayout form = column(); form.setPadding(dp(20), dp(8), dp(20), 0);
        EditText name = addField(form, "Tên danh mục *", original == null ? "" : original.name, false);
        EditText description = addField(form, "Mô tả", original == null ? "" : original.description, true);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(original == null ? "Thêm danh mục" : "Sửa danh mục")
                .setView(form).setNegativeButton("Hủy", null).setPositiveButton("Lưu", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (name.getText().toString().trim().isEmpty()) { name.setError("Nhập tên danh mục."); return; }
            Category c = original == null ? new Category() : original;
            c.name = name.getText().toString(); c.description = description.getText().toString();
            dialog.dismiss(); mutate("Đang lưu danh mục…", api -> { if (original == null) api.createCategory(c); else api.updateCategory(c); }, "Lưu danh mục thành công.");
        }));
        dialog.show();
    }

    private void showCategoryActions(Category category) {
        String[] options = {"Chỉnh sửa danh mục", "Xóa danh mục"};
        new AlertDialog.Builder(this).setTitle(category.name).setItems(options, (d, which) -> {
            if (which == 0) showCategoryForm(category); else confirmDeleteCategory(category);
        }).show();
    }

    private void confirmDeleteCourse(Course c) {
        new AlertDialog.Builder(this).setTitle("Xóa khóa học?")
                .setMessage("“" + c.title + "” sẽ bị xóa khỏi MockAPI.")
                .setNegativeButton("Giữ lại", null).setPositiveButton("Xóa", (d, w) ->
                        { screen = "courses"; selectedCourse = null; mutate("Đang xóa khóa học…", api -> api.deleteCourse(c.id), "Đã xóa khóa học."); }).show();
    }

    private void confirmDeleteCategory(Category category) {
        for (Course c : courses) if (category.id.equals(c.categoryId)) { toast("Danh mục đang được dùng bởi khóa học “" + c.title + "”."); return; }
        new AlertDialog.Builder(this).setTitle("Xóa danh mục?")
                .setMessage("“" + category.name + "” sẽ bị xóa khỏi MockAPI.")
                .setNegativeButton("Hủy", null).setPositiveButton("Xóa", (d, w) ->
                        mutate("Đang xóa danh mục…", api -> api.deleteCategory(category.id), "Đã xóa danh mục.")).show();
    }

    private void loadData() {
        if (baseUrl.isEmpty()) { render(); return; }
        loading = true; render();
        io.execute(() -> {
            try {
                MockApi api = new MockApi(baseUrl);
                List<Category> fetchedCategories = api.getCategories();
                List<Course> fetchedCourses = api.getCourses();
                runOnUiThread(() -> {
                    categories.clear(); categories.addAll(fetchedCategories);
                    courses.clear(); courses.addAll(fetchedCourses);
                    loading = false; render();
                });
            } catch (Exception e) {
                runOnUiThread(() -> { loading = false; render(); toast("Không tải được MockAPI: " + friendlyError(e)); });
            }
        });
    }

    private interface ApiAction { void run(MockApi api) throws Exception; }
    private void mutate(String progress, ApiAction action, String success) {
        toast(progress);
        io.execute(() -> {
            try {
                action.run(new MockApi(baseUrl));
                runOnUiThread(() -> { toast(success); loadData(); });
            } catch (Exception e) { runOnUiThread(() -> toast("Không lưu được: " + friendlyError(e))); }
        });
    }

    private void loadImage(String address, ImageView target) {
        if (address == null || address.trim().isEmpty()) return;
        target.setTag(address);
        io.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(address.trim()).openConnection();
                connection.setConnectTimeout(7000); connection.setReadTimeout(9000);
                Bitmap bitmap = BitmapFactory.decodeStream(connection.getInputStream());
                if (bitmap != null) runOnUiThread(() -> { if (address.equals(target.getTag())) target.setImageBitmap(bitmap); });
            } catch (Exception ignored) { }
            finally { if (connection != null) connection.disconnect(); }
        });
    }

    private void addStatus(String message, boolean isError) {
        TextView status = text(message, 12, isError ? Color.rgb(164, 96, 32) : MUTED, false);
        status.setPadding(dp(14), dp(12), dp(14), dp(12));
        status.setBackground(round(isError ? Color.rgb(255, 247, 231) : Color.WHITE, 13));
        body.addView(status, layout(-1, -2, 0, 13, 0, 0, 0, 0));
    }

    private EditText addField(LinearLayout parent, String label, String value, boolean multiline) {
        TextView title = text(label, 12, MUTED, true);
        parent.addView(title, layout(-1, -2, 0, 7, 0, 5, 0, 0));
        EditText edit = field("", multiline);
        edit.setText(value);
        parent.addView(edit, layout(-1, multiline ? dp(88) : dp(47), 0, 0, 0, 0, 0, 2));
        return edit;
    }

    private EditText field(String hint, boolean multiline) {
        EditText edit = new EditText(this);
        edit.setTextSize(14); edit.setTextColor(INK); edit.setHintTextColor(MUTED); edit.setHint(hint);
        edit.setPadding(dp(13), dp(9), dp(13), dp(9)); edit.setBackground(round(Color.rgb(247, 248, 251), 11));
        if (multiline) { edit.setGravity(Gravity.TOP | Gravity.LEFT); edit.setSingleLine(false); edit.setMinLines(3); }
        else edit.setSingleLine(true);
        return edit;
    }

    private Button actionButton(String label, boolean danger) {
        Button button = new Button(this);
        button.setText(label); button.setTextSize(12); button.setAllCaps(false); button.setTypeface(null, Typeface.BOLD);
        button.setTextColor(danger ? Color.rgb(190, 63, 72) : BLUE);
        button.setBackground(round(danger ? Color.rgb(255, 239, 240) : Color.rgb(235, 241, 255), 12));
        return button;
    }

    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private int dp(float n) { return (int) (n * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(dp(radius)); return drawable;
    }
    private GradientDrawable gradient(int a, int b, int radius) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{a, b});
        drawable.setCornerRadius(dp(radius)); return drawable;
    }
    private LinearLayout.LayoutParams layout(int w, int h, int left, int top, int right, int bottom, int ignored1, int ignored2) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
        lp.setMargins(dp(left), dp(top), dp(right), dp(bottom)); return lp;
    }
    private void hideKeyboard() {
        try { ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(), 0); } catch (Exception ignored) { }
    }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
    private String categoryName(String id) { for (Category c : categories) if (c.id.equals(id)) return c.name; return "Khóa học"; }
    private int courseColor(String id) { int[] colors = {Color.rgb(83, 107, 193), Color.rgb(67, 147, 159), Color.rgb(189, 116, 99), Color.rgb(115, 100, 177)}; return colors[Math.floorMod(id.hashCode(), colors.length)]; }
    private String formatPrice(double price) {
        NumberFormat f = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        f.setMaximumFractionDigits(0); return f.format(price) + " đ";
    }
    private String numberValue(double value) { return value == (long) value ? String.valueOf((long) value) : String.valueOf(value); }
    private String friendlyError(Exception e) {
        String m = e.getMessage() == null ? "Lỗi kết nối." : e.getMessage();
        if (m.contains("Unable to resolve host") || m.contains("Failed to connect") || m.contains("Network is unreachable"))
            return "không có mạng hoặc Base URL chưa đúng.";
        if (m.contains("CLEARTEXT")) return "MockAPI cần URL HTTPS.";
        return m;
    }

    private class HorizontalScroller extends HorizontalScrollView {
        private final LinearLayout chips = row();
        HorizontalScroller(Context context) { super(context); setHorizontalScrollBarEnabled(false); setFillViewport(false); addView(chips); }
        void addChip(String label, String id, boolean active) {
            TextView chip = text(label, 12, active ? Color.WHITE : MUTED, active);
            chip.setGravity(Gravity.CENTER); chip.setPadding(dp(14), 0, dp(14), 0);
            chip.setBackground(round(active ? BLUE : Color.WHITE, 18));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(36)); lp.setMargins(0, 0, dp(8), 0); chips.addView(chip, lp);
            chip.setOnClickListener(v -> { categoryFilter = id; render(); });
        }
    }

    @Override public void onBackPressed() {
        if (screen.equals("detail")) { screen = "courses"; selectedCourse = null; render(); }
        else super.onBackPressed();
    }
}
