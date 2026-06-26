import pandas as pd
import numpy as np
import re
from datetime import datetime
from sklearn.model_selection import train_test_split
from xgboost import XGBRegressor
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from sklearn.impute import SimpleImputer
from sklearn.metrics import r2_score, mean_absolute_error
import joblib

# ===============================
# 1️ Cấu hình chung
# ===============================
CURRENT_YEAR = datetime.now().year
print(" Đang đọc dữ liệu lịch sử...")

try:
    data = pd.read_csv("historical_listings.csv")
    data.columns = data.columns.str.strip() # Đảm bảo tên cột sạch
except FileNotFoundError:
    print("❌ Không tìm thấy file historical_listings.csv")
    exit()

# ===============================
# 2️ Làm sạch dữ liệu
# ===============================
print("🧠 Đang xử lý & làm sạch dữ liệu...")

# Loại bỏ trùng lặp
rows_before = len(data)
data = data.drop_duplicates()
print(f"✅ Đã loại bỏ {rows_before - len(data)} dòng trùng lặp.")

# Loại bỏ giá trị target trống
if "FINAL_SALE_PRICE" not in data.columns:
    print("❌ Không tìm thấy cột FINAL_SALE_PRICE trong dữ liệu!")
    exit()

target_rows_before = len(data)
data = data.dropna(subset=["FINAL_SALE_PRICE"])
print(f"✅ Đã loại bỏ {target_rows_before - len(data)} dòng thiếu FINAL_SALE_PRICE.")

# ===============================
# 3️ Hàm tiện ích trích xuất đặc trưng
# ===============================
def extract_capacity_value(capacity_str):
    """Chuyển đổi dung lượng pin (kWh / Ah) sang số kWh chuẩn."""
    if pd.isna(capacity_str): return 0
    capacity_str = str(capacity_str).lower().replace(" ", "")
    match = re.search(r'(\d+(\.\d+)?)', capacity_str)
    if match:
        value = float(match.group(1))
        if 'kwh' in capacity_str: return value
        if 'ah' in capacity_str:
            # Ước tính điện áp 48V cho pin nhỏ (<100Ah) và 72V cho pin lớn hơn
            if value > 100: return (value * 48) / 1000
            else: return (value * 72) / 1000
        return value
    return 0

def extract_lifespan_months(lifespan_str):
    """Trích xuất số tháng tuổi thọ pin."""
    if pd.isna(lifespan_str): return 0
    match = re.search(r'(\d+)', str(lifespan_str))
    return int(match.group(1)) if match else 0

def extract_charge_time(time_str):
    """Trích xuất số giờ sạc lớn nhất."""
    if pd.isna(time_str):
        return 0
    numbers = re.findall(r'\d+(?:\.\d+)?', str(time_str))
    valid_numbers = [float(n) for n in numbers if n.strip()]
    return max(valid_numbers) if valid_numbers else 0

def calculate_wear_score(row):
    if row.get("productType") in ["bike", "motorbike"]:
        mileage = row.get('mileage', 0)
        cycles = row.get('chargeCycles', 0)
        mileage_factor = 1 / np.log1p(mileage) if mileage > 0 else 1
        cycles_factor = 1 / np.log1p(cycles) if cycles > 0 else 1
        return (mileage_factor * 0.6 + cycles_factor * 0.4)
    return 1

# ===============================
# 4️ Feature Engineering
# ===============================
data["batteryCapacity_numeric"] = data["batteryCapacity"].apply(extract_capacity_value)
data["batteryLifespan_months"] = data["batteryLifespan"].apply(extract_lifespan_months)
data["chargeTime_numeric"] = data["chargeTime"].apply(extract_charge_time)
data["yearOfManufacture_numeric"] = pd.to_numeric(data["yearOfManufacture"], errors="coerce")
data["productType"] = data["productType"].fillna("missing").str.lower()

# Xử lý brand thiếu cho mean encoding
data["brand"] = data["brand"].fillna("missing")

# condition_score: càng mới càng cao
data["condition_score"] = data["conditionId"].apply(lambda x: (5 - x) if x in [1, 2, 3, 4] else 0)

# Tuổi xe
data["age"] = data["yearOfManufacture_numeric"].apply(
    lambda x: (CURRENT_YEAR - x) if pd.notna(x) and x > 1900 else 0
)
data["wear_score"] = data.apply(calculate_wear_score, axis=1)

# CẢI TIẾN 1: Pin Value Density
data['maxSpeed_safe'] = data['maxSpeed'].replace(0, 1e-6) 
data["pin_value_per_speed"] = data["batteryCapacity_numeric"] / data['maxSpeed_safe']

#  CẢI TIẾN 2: Brand Value Score (Mean Encoding)
data['log_price'] = np.log1p(data['FINAL_SALE_PRICE'])
brand_mean_prices = data.groupby('brand')['log_price'].mean()
data['brand_value_score'] = data['brand'].map(brand_mean_prices)

# ===============================
# 5️ Khởi tạo Pipeline & Mô hình
# ===============================
numeric_features = [
    "mileage", "rangePerCharge", "chargeCycles", "batteryCapacity_numeric",
    "batteryLifespan_months", "maxSpeed", "chargeTime_numeric",
    "condition_score", "age", "yearOfManufacture_numeric", "wear_score",
    "pin_value_per_speed", "brand_value_score"
]
categorical_features = [
    "productType", "brand", "warrantyPolicy", "batteryType",
    "color", "compatibleVehicle"
]
TARGET = "FINAL_SALE_PRICE"

numeric_transformer = Pipeline(steps=[
    ("imputer", SimpleImputer(strategy="mean")),
    ("scaler", StandardScaler())
])
categorical_transformer = Pipeline(steps=[
    ("imputer", SimpleImputer(strategy="constant", fill_value="missing")),
    ("onehot", OneHotEncoder(handle_unknown="ignore"))
])
preprocessor = ColumnTransformer(transformers=[
    ("num", numeric_transformer, numeric_features),
    ("cat", categorical_transformer, categorical_features)
])

def create_model_pipeline():
    return Pipeline(steps=[
        ("preprocessor", preprocessor),
        ("regressor", XGBRegressor(
            n_estimators=400,
            learning_rate=0.05,
            max_depth=7,       
            subsample=0.8,
            random_state=42,
            objective='reg:squarederror', 
            n_jobs=-1 
        ))
    ])

# ===============================
# 6️ Phân mảnh Dữ liệu & Huấn luyện
# ===============================
# (Đã gộp 3 cụm CAR thành 2 cụm)
MODEL_CONFIG = {
    'bike': {'filter': ['bike'], 'filename': 'pricing_model_bike.pkl'},
    'motorbike': {'filter': ['motorbike'], 'filename': 'pricing_model_motorbike.pkl'},
    'battery': {'filter': ['battery'], 'filename': 'pricing_model_battery.pkl'},
    
    #  2 SUB-SEGMENT MỚI CHO CAR
    
            'car_low': {
                'filter': lambda df: (df['productType'] == 'car') & (df[TARGET] <= 600_000_000),
                'model_file': 'pricing_model_car_low.pkl',
                'min_price': 0, 'max_price': 600_000_000
            },
            'car_high': {
                'filter': lambda df: (df['productType'] == 'car') & (df[TARGET] > 600_000_000),
                'model_file': 'pricing_model_car_high.pkl',
                'min_price': 600_000_000, 'max_price': float('inf')
            },

    
    # Mô hình dự phòng cho 'other' và 'missing'
    'missing': {'filter': ['other', 'missing'], 'filename': 'pricing_model_other.pkl'}, 
}

all_results = {}

print("\n⚙️ Bắt đầu huấn luyện các mô hình chuyên biệt (Áp dụng Phân Cụm 2 CAR)...")

for name, config in MODEL_CONFIG.items():
    
    if callable(config['filter']):
        # Xử lý filter lambda cho CAR
   
        data_segment = data[config['filter'](data)].copy()
    else:
        # Xử lý filter list thông thường
        filter_types = config['filter']
        data_segment = data[data['productType'].isin(filter_types)].copy() 

    # Bỏ qua nếu không đủ dữ liệu
    if data_segment.empty or len(data_segment) < 20: 
        print(f"⚠️ Bỏ qua '{name}': Không đủ dữ liệu ({len(data_segment)} dòng).")
        continue

    X_segment = data_segment[numeric_features + categorical_features]
    y_segment = np.log1p(data_segment[TARGET])
    
    # Tạo và huấn luyện mô hình
    model = create_model_pipeline()
    
    # Chia dữ liệu và train
    X_train, X_test, y_train, y_test = train_test_split(
        X_segment, y_segment, test_size=0.2, random_state=42
    )
    model.fit(X_train, y_train)

    # Đánh giá
    y_pred = model.predict(X_test)
    r2 = r2_score(y_test, y_pred)
    mae_price = mean_absolute_error(np.expm1(y_test), np.expm1(y_pred))

    all_results[name] = {'R2': r2, 'MAE': mae_price, 'Count': len(data_segment)}
    
    # Lưu mô hình
    model_file = config.get('filename', config.get('model_file')) # Tương thích cả 2 key
    joblib.dump(model, model_file)
    print(f"✅ Đã lưu mô hình '{name}' ({model_file}).")

# ===============================
# 7️ Tổng kết
# ===============================
print("\n📊 === TỔNG KẾT KẾT QUẢ CÁC MÔ HÌNH SAU KHI GỘP CỤM 2 CAR ===")
for name, res in all_results.items():
    print(f"[{name.upper()}] (N={res['Count']}): R²={res['R2']:.4f}, MAE={res['MAE']:,.0f} VNĐ")

print("\n🚀 Huấn luyện hoàn tất! Sẵn sàng chạy Flask API.")