"""
predict_admission.py — ARIMA + XGBoost 预测未来 3 年招生规模

Usage:
  HOME=... USERPROFILE=... python predict_admission.py --yearly "2020,2021,2022,2023,2024,2025" --counts "100,100,100,100,50"

Output (stdout):
  {"predictions": [...], "historical": [...], "metrics": {...}}
"""

import json
import sys
import argparse
import warnings
warnings.filterwarnings('ignore')
import logging
logging.getLogger('statsmodels').setLevel(logging.ERROR)
logging.getLogger('xgboost').setLevel(logging.ERROR)

import numpy as np
import pandas as pd

# ---------- ARIMA ----------
def train_arima(series):
    from statsmodels.tsa.arima.model import ARIMA
    best_aic = float('inf')
    best_order = None
    best_model = None

    for p in range(0, 4):
        for d in range(0, 2):
            for q in range(0, 4):
                try:
                    model = ARIMA(series, order=(p, d, q))
                    result = model.fit()
                    if result.aic < best_aic:
                        best_aic = result.aic
                        best_order = (p, d, q)
                        best_model = result
                except Exception:
                    continue

    return best_model, best_order

# ---------- XGBoost ----------
def build_xgb_features(yearly_df):
    df = yearly_df.copy()
    df['lag_1'] = df['count'].shift(1)
    df['lag_2'] = df['count'].shift(2)
    df['growth'] = df['count'].pct_change()
    df['growth_lag1'] = df['growth'].shift(1)
    df = df.dropna().reset_index(drop=True)
    return df

def train_xgboost(feature_df):
    import xgboost as xgb
    from sklearn.metrics import mean_absolute_percentage_error

    X = feature_df[['year_idx', 'lag_1', 'lag_2', 'growth', 'growth_lag1']].values
    y = feature_df['count'].values

    model = xgb.XGBRegressor(
        n_estimators=50,
        max_depth=3,
        learning_rate=0.1,
        random_state=42,
        verbosity=0,
    )

    if len(X) >= 3:
        model.fit(X, y)
        train_pred = model.predict(X)
        mape = mean_absolute_percentage_error(y, train_pred) * 100
    else:
        mape = None

    return model, mape

def predict_xgboost(model, last_df, n_years):
    preds = []
    df = last_df.copy()
    for i in range(n_years):
        year_idx = len(df) + i
        lag_1 = df['count'].iloc[-1] if len(preds) == 0 else preds[-1]
        lag_2 = df['count'].iloc[-2] if len(preds) == 0 else (preds[-2] if len(preds) >= 2 else df['count'].iloc[-2])
        growth = (lag_1 / df['count'].iloc[-2] - 1) if df['count'].iloc[-2] > 0 else 0
        growth_lag1 = df['growth'].iloc[-1] if 'growth' in df.columns and len(df) > 0 else 0

        X_pred = np.array([[year_idx, lag_1, lag_2, growth, growth_lag1]])
        val = max(0, float(model.predict(X_pred)[0]))
        val = int(round(val))
        preds.append(val)
    return preds

# ---------- Ensemble ----------
def ensemble_predict(arima_forecast, xgb_forecast, arima_aic=None, xgb_mape=None):
    if arima_forecast is None and xgb_forecast is None:
        return None
    if arima_forecast is None:
        return xgb_forecast
    if xgb_forecast is None:
        return arima_forecast

    n = len(arima_forecast)
    result = []
    for i in range(n):
        val = int(round(0.5 * arima_forecast[i] + 0.5 * xgb_forecast[i]))
        result.append(max(0, val))
    return result

# ---------- Main ----------
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--yearly', required=True, help='Comma-separated years')
    parser.add_argument('--counts', required=True, help='Comma-separated counts')
    parser.add_argument('--years_ahead', type=int, default=3, help='Years to predict')
    args = parser.parse_args()

    years = [int(y) for y in args.yearly.split(',')]
    counts = [int(c) for c in args.counts.split(',')]

    if len(years) < 3:
        print(json.dumps({"error": "至少需要3年数据"}))
        sys.exit(1)

    series = pd.Series(counts, dtype=float)
    df = pd.DataFrame({'year': years, 'count': counts})
    df['year_idx'] = range(len(df))

    # ARIMA
    arima_model, arima_order = train_arima(series)
    arima_forecast = None
    if arima_model is not None:
        raw_fc = arima_model.forecast(steps=args.years_ahead)
        arima_forecast = [int(round(max(0, v))) for v in raw_fc.tolist()]

    # XGBoost
    xgb_model = None
    xgb_forecast = None
    xgb_mape = None
    feature_df = build_xgb_features(df)
    if len(feature_df) >= 3:
        xgb_model, xgb_mape = train_xgboost(feature_df)
        if xgb_model is not None:
            xgb_forecast = predict_xgboost(xgb_model, feature_df, args.years_ahead)

    # Ensemble
    preds = ensemble_predict(arima_forecast, xgb_forecast)

    # 历史数据
    historical = [{"year": y, "actual": c} for y, c in zip(years, counts)]

    # 预测结果 + 置信区间
    predictions = []
    if preds:
        residuals = []
        if arima_model is not None:
            residuals = arima_model.resid.tolist()
        resid_std = np.std(residuals) if len(residuals) > 1 else np.mean(counts) * 0.1

        for i, (val, yr) in enumerate(zip(preds, [years[-1] + j + 1 for j in range(args.years_ahead)])):
            ci = int(round(1.96 * resid_std * (1 + i * 0.3)))
            predictions.append({
                "year": yr,
                "predicted": val,
                "lowerBound": max(0, val - ci),
                "upperBound": val + ci,
            })

    # 精度指标
    metrics = {}
    if arima_model is not None:
        from sklearn.metrics import mean_absolute_percentage_error, mean_absolute_error
        fitted = arima_model.fittedvalues
        valid_mask = ~np.isnan(fitted)
        if valid_mask.sum() > 0:
            y_true = series.values[valid_mask]
            y_pred = fitted[valid_mask]
            metrics["mape"] = round(float(mean_absolute_percentage_error(y_true, y_pred) * 100), 2)
            metrics["mae"] = round(float(mean_absolute_error(y_true, y_pred)), 2)
            metrics["rmse"] = round(float(np.sqrt(np.mean((y_true - y_pred) ** 2))), 2)
            metrics["arima_order"] = str(arima_order)

    output = {
        "historical": historical,
        "predictions": predictions,
        "metrics": metrics,
    }

    print(json.dumps(output, ensure_ascii=False))

if __name__ == '__main__':
    main()
