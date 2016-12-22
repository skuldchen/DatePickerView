package com.example.hsiangyuchen.customdatepiacker;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.IntDef;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

/**
 * Created by hsiangyuchen on 2016/12/16.
 */

public class DatePickerView extends RelativeLayout {
    //  DatePickerView Type
    public static final int DATEPICKERVIEW_TYPE_BIRTHDAYPICKER = 0;
    public static final int DATEPICKERVIEW_TYPE_CARDEXPIRYPICKER = 1;

    // Dummy data, in order for recyclerview can scroll the top and the bottom data to center
    private static final int DUMMY_DATA_COUNT = 2;
    private static final String DUMMY_DATA = "";

    // Constant
    private final static int MAX_MONTH_COUNT = 12;
    private final static int BIRTHDAY_YEAR_RANGE = 150;
    private final static int CARD_EXPIRY_YEAR_RANGE = 10;


    @IntDef({
            DATEPICKERVIEW_TYPE_BIRTHDAYPICKER,
            DATEPICKERVIEW_TYPE_CARDEXPIRYPICKER

    })
    public @interface DatePickerViewType {

    }

    // UI widgets
    private Button button_cancel;
    private Button button_done;
    private RecyclerView recyclerView_month;
    private RecyclerView recyclerView_date;
    private RecyclerView recyclerView_year;
    private RelativeLayout relativeLayout_date;
    private TextView textView_month;
    private TextView textView_date;
    private TextView textView_year;
    private TextView textView_slash;

    // Callback
    private DatePickerListener mDatePickerListener;

    // Adapter
    private DateAdapter dateAdapter;
    private MonthAdapter monthAdapter;
    private YearAdapter yearAdapter;

    // Date
    private int selectDate;
    private int selectYear;
    private int selectMonth;
    private int todayDate;
    private int todayMonth;
    private int todayYear;
    private int birthdayMaxDate;
    private int birthdayMaxMonth;
    private int birthdayMaxYear;
    private int birthdayMinDate;
    private int birthdayMinMonth;
    private int creditCardMaxYear;
    private int creditCardMinYear;
    private int birthdayMinYear;
    private long mSelectUnixTime;
    private long mMaxUnixTime;
    private long mMinUnixTime;


    // ViewType
    private int mDatePickerViewType;

    // Datesoure
    private ArrayList<String> monthArrayList = new ArrayList();
    private ArrayList<String> dateArrayList = new ArrayList<>();
    private ArrayList<String> yearArrayList = new ArrayList<>();

    // flag
    private int nowDateMiddlePosition;


    /* ------------------------------ Interface */
    public interface DatePickerListener {
        void onDone();

        void onCancel();
    }

    public void setDatePickerListener(DatePickerListener datePickerListener) {
        mDatePickerListener = datePickerListener;
    }

    /* ------------------------------ Constructor */
    public DatePickerView(Context context, int mDatePickerViewType, long selectUnixTime, long minUnixTime, long maxUnixTime) {
        super(context);
        this.mDatePickerViewType = mDatePickerViewType;
        this.mSelectUnixTime = selectUnixTime;
        this.mMinUnixTime = minUnixTime;
        this.mMaxUnixTime = maxUnixTime;
        initDate();
        initViews();


    }

    public DatePickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews();

    }

    public DatePickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews();
    }

    /* ------------------------------ Views */
    private void initViews() {
        inflate(getContext(), R.layout.layout_datepicker, this);
        textView_date = (TextView) findViewById(R.id.textview_date);
        textView_month = (TextView) findViewById(R.id.textview_month);
        textView_year = (TextView) findViewById(R.id.textview_year);
        textView_slash = (TextView) findViewById(R.id.textview_slash);
        relativeLayout_date = (RelativeLayout) findViewById(R.id.relativelayout_date);
        recyclerView_month = (RecyclerView) findViewById(R.id.recyclerview_month);
        recyclerView_date = (RecyclerView) findViewById(R.id.recyclerview_date);
        recyclerView_year = (RecyclerView) findViewById(R.id.recyclerview_year);
        button_cancel = (Button) findViewById(R.id.button_cancel);
        button_done = (Button) findViewById(R.id.button_done);
        button_cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDatePickerListener != null) {
                    mDatePickerListener.onCancel();
                }
            }
        });
        button_done.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDatePickerListener != null) {
                    mDatePickerListener.onDone();
                }
            }
        });
        LinearLayoutManager linearLayoutManagerMonth = new LinearLayoutManager(getContext());
        LinearLayoutManager linearLayoutManagerDay = new LinearLayoutManager(getContext());
        LinearLayoutManager linearLayoutManagerYear = new LinearLayoutManager(getContext());
        recyclerView_month.setLayoutManager(linearLayoutManagerMonth);
        recyclerView_date.setLayoutManager(linearLayoutManagerDay);
        recyclerView_year.setLayoutManager(linearLayoutManagerYear);
        dateAdapter = new DateAdapter(generateDateDataSource(selectYear, selectMonth), dpToPx(120));

        switch (mDatePickerViewType) {
            case DATEPICKERVIEW_TYPE_BIRTHDAYPICKER:
                yearAdapter = new YearAdapter(generateYearDataSourceForBirthdayDatePicker(), dpToPx(120));
                monthAdapter = new MonthAdapter(generateMonthDataSourseForBirthdayDatePicker(selectYear), dpToPx(120));
                break;
            case DATEPICKERVIEW_TYPE_CARDEXPIRYPICKER:
                relativeLayout_date.setVisibility(GONE);
                textView_date.setVisibility(GONE);
                textView_slash.setVisibility(VISIBLE);
                yearAdapter = new YearAdapter(generateYearDataSourceForCardExpiryDatePicker(), dpToPx(120));
                monthAdapter = new MonthAdapter(generateMonthDataSourseForCardExpiryDatePicker(Integer.parseInt(getWeekYearFormat(selectYear))), dpToPx(120));
                break;

        }

        recyclerView_month.setAdapter(monthAdapter);
        recyclerView_date.setAdapter(dateAdapter);
        recyclerView_year.setAdapter(yearAdapter);
        addRecyclerviewScrollListener();
        setRecyclerviewScrollToSpecifyDate();
        setDisplayDate();

    }

    /* ------------------------------ Date */
    private void initDate() {
        Calendar calendarSelect = Calendar.getInstance();
        calendarSelect.setTimeInMillis(mSelectUnixTime);
        selectDate = calendarSelect.get(Calendar.DAY_OF_MONTH);
        selectMonth = calendarSelect.get(Calendar.MONTH);
        selectYear = calendarSelect.get(Calendar.YEAR);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        todayDate = calendar.get(Calendar.DATE);
        todayMonth = calendar.get(Calendar.MONTH);
        todayYear = calendar.get(Calendar.YEAR);
        Calendar calendarMax = Calendar.getInstance();
        calendarMax.setTimeInMillis(mMaxUnixTime);
        Calendar calendarMin = Calendar.getInstance();
        calendarMin.setTimeInMillis(mMinUnixTime);
        switch (mDatePickerViewType) {
            case DATEPICKERVIEW_TYPE_BIRTHDAYPICKER:
                //  Set Min birthday date, if mMinUnixTime=0, birthdayMinDate=todayDate, birthdayMinMonth=todayMonth, birthdayMinYear= todayYear-150
                if (mMinUnixTime == 0) {
                    birthdayMinDate = todayDate;
                    birthdayMinMonth = todayMonth;
                    birthdayMinYear = todayYear - BIRTHDAY_YEAR_RANGE;
                } else {
                    birthdayMinDate = calendarMin.get(Calendar.DAY_OF_MONTH);
                    birthdayMinMonth = calendarMin.get(Calendar.MONTH);
                    birthdayMinYear = calendarMin.get(Calendar.YEAR);
                }
                //  Set Max birthday date, if mMaxUnixTime=0, birthdayMaxDate, birthdayMaxMonth, birthdayMaxYear = today
                if (mMaxUnixTime == 0) {
                    birthdayMaxDate = todayDate;
                    birthdayMaxMonth = todayMonth;
                    birthdayMaxYear = todayYear;
                } else {
                    birthdayMaxDate = calendarMax.get(Calendar.DAY_OF_MONTH);
                    birthdayMaxMonth = calendarMax.get(Calendar.MONTH);
                    birthdayMaxYear = calendarMax.get((Calendar.YEAR));
                }

                break;
            case DATEPICKERVIEW_TYPE_CARDEXPIRYPICKER:
                if (mMinUnixTime == 0) {
                    creditCardMinYear = todayYear;
                } else {
                    creditCardMinYear = calendarMin.get(Calendar.YEAR);
                }
                if (mMaxUnixTime == 0) {
                    creditCardMaxYear = todayYear + CARD_EXPIRY_YEAR_RANGE;
                } else {
                    creditCardMaxYear = calendarMax.get(Calendar.YEAR);
                }
                break;
        }


    }

    /* ------------------------------ Add recyclerview scroll listener */

    private void addRecyclerviewScrollListener() {
        recyclerView_month.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    monthAdapter.setHighlightItem(getMonthMiddlePosition());
                    ((LinearLayoutManager) recyclerView_month.getLayoutManager()).scrollToPositionWithOffset(getMonthScrollPosition(), 0);
                    //  Month is calculated form 0, so need to -1, and when state is idle, the date data source need to refresh
                    dateAdapter.setDateSource(generateDateDataSource(Integer.parseInt(yearArrayList.get(getYearMiddlePostion())), getMonthMiddlePosition() - 1));
                    refreshDateMiddlePosition();
                    textView_month.setText(monthAdapter.getHighlightItem());


                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        recyclerView_date.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);


            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    dateAdapter.setHighlightItem(getDateMiddlePostion());
                    ((LinearLayoutManager) recyclerView_date.getLayoutManager()).scrollToPositionWithOffset(getDateScrollPosition(), 0);
                    textView_date.setText(dateAdapter.getHighlightItem());

                }
            }
        });

        recyclerView_year.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    yearAdapter.setHighlightItem(getYearMiddlePostion());
                    ((LinearLayoutManager) recyclerView_year.getLayoutManager()).scrollToPositionWithOffset(getYearScrollPosition(), 0);
                    switch (mDatePickerViewType) {
                        case DATEPICKERVIEW_TYPE_BIRTHDAYPICKER:
                            monthAdapter.setDateSource(generateMonthDataSourseForBirthdayDatePicker(Integer.parseInt(yearArrayList.get(getYearMiddlePostion()))));
                            dateAdapter.setDateSource(generateDateDataSource(Integer.parseInt(yearArrayList.get(getYearMiddlePostion())), getMonthMiddlePosition() - 1));
                            refreshDateMiddlePosition();
                            textView_year.setText(yearAdapter.getHighlightItem());
                            break;
                        case DATEPICKERVIEW_TYPE_CARDEXPIRYPICKER:
                            String nowSelectMonth = monthArrayList.get(getMonthMiddlePosition());
                            monthAdapter.setDateSource(generateMonthDataSourseForCardExpiryDatePicker(Integer.parseInt(yearArrayList.get(getYearMiddlePostion()))));
                            refreshMonthMiddlePositionForCardExpiryDatePicker(nowSelectMonth);
                            textView_year.setText(yearAdapter.getHighlightItem());
                            break;
                    }


                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
    }

    /* ------------------------------ Get MonthAdapter middle position */
    private int getMonthMiddlePosition() {
        return getMonthScrollPosition() + (monthAdapter.ITEM_NUM / 2);
    }

    /* ------------------------------ Get MonthAdapter scroll position */
    private int getMonthScrollPosition() {
        return (int) (((double) recyclerView_month.computeVerticalScrollOffset()
                / (double) monthAdapter.getItemHeight()) + 0.5f);
    }

    /* ------------------------------ Get DateAdapter middle position */

    private int getDateMiddlePostion() {
        return getDateScrollPosition() + (dateAdapter.ITEM_NUM / 2);
    }

    /* ------------------------------ Get DateAdapter scroll position */

    private int getDateScrollPosition() {
        return (int) (((double) recyclerView_date.computeVerticalScrollOffset()
                / (double) dateAdapter.getItemHeight()) + 0.5f);
    }

     /* ------------------------------ Get YearAdapter middle position */

    private int getYearMiddlePostion() {
        return getYearScrollPosition() + (dateAdapter.ITEM_NUM / 2);
    }

    /* ------------------------------ Get YearAdapter scroll position */

    private int getYearScrollPosition() {
        return (int) (((double) recyclerView_year.computeVerticalScrollOffset()
                / (double) yearAdapter.getItemHeight()) + 0.5f);
    }


    /**
     * Generate Month Data Source For BirthdayDatePicker
     *
     * @param selectYear the year you select
     */
    public ArrayList<String> generateMonthDataSourseForBirthdayDatePicker(int selectYear) {
        DateFormatSymbols dateFormatSymbols = new DateFormatSymbols(Locale.getDefault());
        monthArrayList.clear();
        String[] monthArray = dateFormatSymbols.getShortMonths();
        int monthCount = 0;
        if (selectYear == birthdayMaxYear) {
            monthCount = todayMonth + 1;
        } else {
            monthCount = MAX_MONTH_COUNT;
        }
        for (int i = 0; i < monthCount + DUMMY_DATA_COUNT; i++) {
            if (i == 0) {
                monthArrayList.add(i, DUMMY_DATA);
            } else if (i == monthCount + DUMMY_DATA_COUNT - 1) {
                monthArrayList.add(i, DUMMY_DATA);
            } else {
                monthArrayList.add(i, monthArray[i - 1]);
            }
        }


        return monthArrayList;
    }

    /**
     * Generate Month Data Source For CardExpiry
     *
     * @param selectYear the year you select, and the format is weekyear 2016 -> 16
     */
    public ArrayList<String> generateMonthDataSourseForCardExpiryDatePicker(int selectYear) {
        monthArrayList.clear();
        int monthCount = 0;
        // Need to check just in few months
        if (selectYear == Integer.valueOf(getWeekYearFormat(creditCardMinYear))) {
            monthCount = (MAX_MONTH_COUNT - (todayMonth + 1)) + 1;
            for (int i = 0; i < monthCount + DUMMY_DATA_COUNT; i++) {
                if (i == 0) {
                    monthArrayList.add(i, DUMMY_DATA);
                } else if (i == monthCount + DUMMY_DATA_COUNT - 1) {
                    monthArrayList.add(i, DUMMY_DATA);
                } else {
                    monthArrayList.add(i, getMonthFormatForCardExpiry(todayMonth));
                }
            }
        } else {
            for (int i = 0; i < MAX_MONTH_COUNT + DUMMY_DATA_COUNT; i++) {
                if (i == 0) {
                    monthArrayList.add(i, DUMMY_DATA);
                } else if (i == MAX_MONTH_COUNT + DUMMY_DATA_COUNT - 1) {
                    monthArrayList.add(i, DUMMY_DATA);
                } else {
                    //  because dummy data and month is calculated from 0, month need to - 1
                    monthArrayList.add(i, getMonthFormatForCardExpiry(i - 1));


                }
            }
        }
        return monthArrayList;
    }

    /**
     * Generate Date Data Sourece and
     *
     * @param selectYear  now you select year
     * @param selectMonth now you select month
     */

    public ArrayList<String> generateDateDataSource(int selectYear, int selectMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, selectMonth);
        int dayOfMonth;
        // If selectYear == birthdayYeat and selectMonth == birthdayMaxMonth , the day's range can't exceed today
        if (selectYear == birthdayMaxYear && selectMonth == birthdayMaxMonth) {
            dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        } else {
            dayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        }
        dateArrayList.clear();
        for (int i = 0; i < dayOfMonth + DUMMY_DATA_COUNT; i++) {
            if (i == 0) {
                dateArrayList.add(i, DUMMY_DATA);
            } else if (i == dayOfMonth + 1) {
                dateArrayList.add(i, DUMMY_DATA);
            } else {
                dateArrayList.add(i, String.valueOf(i));
            }
        }


        return dateArrayList;
    }

    /**
     * Generate Year Data Source for BirthdayDatePicker
     */

    public ArrayList<String> generateYearDataSourceForBirthdayDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        yearArrayList.clear();
        for (int i = 0; i < ((birthdayMaxYear - birthdayMinYear + 1) + DUMMY_DATA_COUNT); i++) {
            if (i == 0) {
                yearArrayList.add(DUMMY_DATA);
            } else if (i == ((birthdayMaxYear - birthdayMinYear + 1) + DUMMY_DATA_COUNT - 1)) {
                yearArrayList.add(DUMMY_DATA);
            } else {
                // +1, because has dummy data in the top
                yearArrayList.add(i, String.valueOf(calendar.get(Calendar.YEAR) - i + 1));
            }

        }

        Collections.reverse(yearArrayList);
        return yearArrayList;

    }

    /**
     * Generate Year Data Source for CardExpiryDatePicker
     * The year range from this year to this year + 10
     */
    public ArrayList<String> generateYearDataSourceForCardExpiryDatePicker() {
        Calendar calendar = Calendar.getInstance();
        yearArrayList.clear();
        for (int i = 0; i < (creditCardMaxYear - creditCardMinYear + DUMMY_DATA_COUNT); i++) {
            if (i == 0) {
                yearArrayList.add(i, DUMMY_DATA);
            } else if (i == creditCardMaxYear - creditCardMinYear + DUMMY_DATA_COUNT - 1) {
                yearArrayList.add(i, DUMMY_DATA);
            } else {
                String weekYear = getWeekYearFormat(calendar.get(Calendar.YEAR) + i - 1);
                yearArrayList.add(i, weekYear);

            }


        }
        return yearArrayList;

    }


    /**
     * Set recyclerview sroll to specify date at first
     */
    public void setRecyclerviewScrollToSpecifyDate() {
        switch (mDatePickerViewType) {
            case DATEPICKERVIEW_TYPE_BIRTHDAYPICKER:
                ((LinearLayoutManager) recyclerView_date.getLayoutManager()).scrollToPositionWithOffset(selectDate - 1, 0);
                dateAdapter.setHighlightItem(selectDate);
                ((LinearLayoutManager) recyclerView_month.getLayoutManager()).scrollToPositionWithOffset(selectMonth, 0);
                monthAdapter.setHighlightItem(selectMonth + 1);
                int yearPosistion = yearArrayList.indexOf(String.valueOf(selectYear));
                ((LinearLayoutManager) recyclerView_year.getLayoutManager()).scrollToPositionWithOffset(yearPosistion - 1, 0);
                yearAdapter.setHighlightItem(yearPosistion);
                break;
            case DATEPICKERVIEW_TYPE_CARDEXPIRYPICKER:
                // monthArrayList is 01, 02, 03 ..., need to convert selectMonth format
                // Because month is calculated from 0, the selectMonth + 1
                String selectMonthStr = getMonthFormatForCardExpiry(selectMonth);
                int monthPosistion = monthArrayList.indexOf(selectMonthStr);
                ((LinearLayoutManager) recyclerView_month.getLayoutManager()).scrollToPositionWithOffset(monthPosistion - 1, 0);
                monthAdapter.setHighlightItem(monthPosistion);
                int yearPosition = yearArrayList.indexOf(getWeekYearFormat(selectYear));
                ((LinearLayoutManager) recyclerView_year.getLayoutManager()).scrollToPositionWithOffset(yearPosition - 1, 0);
                yearAdapter.setHighlightItem(yearPosition);
                break;


        }

    }

    /**
     * Set textview_date, text_month, text_year to specify date at first
     */
    public void setDisplayDate() {
        switch (mDatePickerViewType) {
            case DATEPICKERVIEW_TYPE_BIRTHDAYPICKER:
                textView_year.setText(String.valueOf(selectYear));
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.MONTH, selectMonth);
                String monthShort = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
                int monthPositionForBirthday = monthArrayList.indexOf(monthShort);
                textView_month.setText(monthArrayList.get(monthPositionForBirthday));
                textView_date.setText(String.valueOf(selectDate));
                break;
            case DATEPICKERVIEW_TYPE_CARDEXPIRYPICKER:
                textView_year.setText(getWeekYearFormat(selectYear));
                int monthPositionForCreditCard = monthArrayList.indexOf(getMonthFormatForCardExpiry(selectMonth));
                textView_month.setText(monthArrayList.get(monthPositionForCreditCard));
                break;

        }

    }

    /**
     * Get select date and convert it to unix time
     */
    public long getSelectDateUnixTime() {
        Calendar calendar = Calendar.getInstance();
        Date date = null;
        switch (mDatePickerViewType) {
            case DATEPICKERVIEW_TYPE_BIRTHDAYPICKER:
                SimpleDateFormat simpleDateFormatForBirthday = new SimpleDateFormat("yyyyMMMdd");
                try {
                    date = simpleDateFormatForBirthday.parse(yearAdapter.getHighlightItem() + monthAdapter.getHighlightItem() + dateAdapter.getHighlightItem());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                calendar.setTime(date);
                break;
            case DATEPICKERVIEW_TYPE_CARDEXPIRYPICKER:
                SimpleDateFormat simpleDateFormatForCardExpiry = new SimpleDateFormat("yyMM");
                try {
                    date = simpleDateFormatForCardExpiry.parse(yearAdapter.getHighlightItem() + monthAdapter.getHighlightItem());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                calendar.setTime(date);
                break;

        }
        long dateUnixTime = calendar.getTimeInMillis();
        return dateUnixTime;
    }

    /**
     * Refresh date middle position
     * When month or year update, the date need to refresh
     * For example, November has 30 days, and October has 31 days, thus the date data source will change
     * and recyclerview_date need to scroll to bottom, and highlight the last date.
     */
    public void refreshDateMiddlePosition() {
        nowDateMiddlePosition = getDateMiddlePostion();
        if (nowDateMiddlePosition > dateArrayList.size() - DUMMY_DATA_COUNT) {
            recyclerView_date.scrollToPosition(dateArrayList.size() - 1);
            dateAdapter.setHighlightItem(dateArrayList.size() - DUMMY_DATA_COUNT);
            nowDateMiddlePosition = dateArrayList.size() - DUMMY_DATA_COUNT;
            textView_date.setText(String.valueOf(nowDateMiddlePosition));
        } else {
            dateAdapter.setHighlightItem(nowDateMiddlePosition);

        }
    }


    /**
     * Refresh month middle position for card expiry date picker
     * When yearupdate, the month need to refresh
     * For example, if you now select 04/17 , when you scroll year to 16 (the montth just have December),
     * the new monthArrayList didn't has the month 04, need to scroll to the 12(todayMonth)
     */

    public void refreshMonthMiddlePositionForCardExpiryDatePicker(String nowSelectMonth) {
        int monthPosition = monthArrayList.indexOf(nowSelectMonth);

        if (monthPosition != -1) {
            ((LinearLayoutManager) recyclerView_month.getLayoutManager()).scrollToPositionWithOffset(monthPosition - 1, 0);
            monthAdapter.setHighlightItem(monthPosition);
        } else {
            // monthArrayList is 01, 02, 03 ..., need to convert selectMonth format
            // Because month is calculated from 0, the selectMonth + 1
            String todayMonthStr = getMonthFormatForCardExpiry(todayMonth);
            int todayMonthPostion = monthArrayList.indexOf(todayMonthStr);
            recyclerView_month.scrollToPosition(todayMonthPostion);
            monthAdapter.setHighlightItem(todayMonthPostion);
            textView_month.setText(todayMonthStr);
        }
    }

    /**
     * Convert year to week year, for example 2016 -> 16
     *
     * @param year, for example 2016
     */

    public String getWeekYearFormat(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy"); // Just the year, with 2 digits
        String weekYear = simpleDateFormat.format(calendar.getTime());
        return weekYear;
    }

    /**
     * Convert monthFormat for CardExpiry
     * In order the CardExpiryDatePickerType's month format is 01,02,03.......10,11,12
     *
     * @param month, for example 0,1..10,11
     */

    public String getMonthFormatForCardExpiry(int month) {
        String monthStr;
        //  Because month is calcalated from 0, need to +1
        if ((month + 1) / 10 == 0) {
            monthStr = String.valueOf(0) + String.valueOf(month + 1);
        } else {
            monthStr = String.valueOf(month + 1);
        }
        return monthStr;
    }

    /**
     * Convert dp to px
     *
     * @param dp the dp you want to convert (in this view we fixed 150dp)
     */
    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }


}
