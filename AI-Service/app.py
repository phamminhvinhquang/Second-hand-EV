import pika
import json
import joblib
import pandas as pd
import re
import numpy as np
from datetime import datetime
import sys
import traceback
import os  

# --- CÁC BIẾN TOÀN CỤC VÀ LOGIC TỪ APP.PY ---
CURRENT_YEAR = datetime.now().year

# Ánh xạ 2 mô hình CAR mới
MODEL_MAP = {
    'bike': 'pricing_model_bike.pkl',
    'motorbike': 'pricing_model_motorbike.pkl',
    'car_low': 'pricing_model_car_low.pkl',
    'car_high': 'pricing_model_car_high.pkl',
    'battery': 'pricing_model_battery.pkl',
    'other': 'pricing_model_other.pkl',
    'missing': 'pricing_model_other.pkl',
}

LOADED_MODELS = {}

# Dữ liệu Mean Encoding (Giữ nguyên)
GLOBAL_BRAND_MEAN_LOG_PRICE = 18.10701871833586
BRAND_MEAN_LOG_PRICES = {
    'Ado': 15.693924155104074, 'Asama': 15.566611387648745, 'Audi': 20.36949746138378, 'BMW': 20.96284874570941, 'BYD': 18.885734945275573, 'CALB': 17.20514810640709, 'CATL': 17.258820534464625, 'DKBike': 17.142810444704818, 'Dat Bike': 17.382610550097446, 'Dibao': 16.801406339263472, 'Engwe': 16.03644994630763, 'Eve Energy': 17.22861938142199, 'Giant': 16.01571338996727, 'Gogoro': 16.784698143033715, 'Gotion': 17.219541647258488, 'Gotion High-Tech': 18.028128926938688, 'Himo': 16.113998553160574, 'Honda': 19.810690081345292, 'Hyundai': 20.248506822512162, 'Kia': 20.072135262785356, 'LG Chem': 17.22153903877945, 'Lishen': 16.700635642284034, 'MG': 19.3265542397311, 'Mercedes': 21.06058025428384, 'Nissan': 19.46970095665683, 'Niu': 16.915823100200424, 'Panasonic': 17.21659759606634, 'Pega': 17.10159037922259, 'Phylion': 16.44264006813076, 'Porsche': 20.302818125880172, 'SK On': 17.596106949573056, 'Samsung': 17.49560067924235, 'Samsung SDI': 17.674055228765738, 'Specialized': 16.609769705896483, 'Tesla': 20.352466279153568, 'Trek': 16.577936508904923, 'Vinfast': 19.139803009744618, 'Vinfast Klara': 17.07699451311597, 'Wuling': 19.894476872100956, 'Xiaomi': 16.2161119184011, 'Yadea': 17.18303850026512, 'Zero': 17.486405551979658, 'missing': 18.028128926938688
}

# Ngưỡng Giá Logarithmic Dựa trên Brand Value Score
CAR_LOG_THRESHOLD_LOW = 20.21 # Tương đương 600M

# Định nghĩa tên Queue (phải khớp với Java)
AI_REQUEST_QUEUE = 'ai.price.request.queue'

# --- HÀM XỬ LÝ DỮ LIỆU (Giữ nguyên từ app.py) ---
def extract_capacity_value(capacity_str):
    if pd.isna(capacity_str): return 0
    capacity_str = str(capacity_str).lower().replace(" ", "")
    match = re.search(r'(\d+(\.\d+)?)', capacity_str)
    if not match: return 0
    value = float(match.group(1))
    if 'kwh' in capacity_str: return value
    if 'ah' in capacity_str:
        return (value * 48) / 1000 if value > 100 else (value * 72) / 1000
    return value

def extract_lifespan_months(lifespan_str):
    if pd.isna(lifespan_str): return 0
    match = re.search(r'(\d+)', str(lifespan_str))
    return int(match.group(1)) if match else 0

def extract_charge_time(time_str):
    if pd.isna(time_str): return 0
    numbers = re.findall(r'\d+(?:\.\d+)?', str(time_str))
    valid_numbers = [float(n) for n in numbers if n.strip()]
    return max(valid_numbers) if valid_numbers else 0

def calculate_wear_score(row):
    # Dùng .get() để an toàn khi key không tồn tại
    productType = row.get("productType")
    if productType in ["bike", "motorbike"]:
        
        # === SỬA LỖI TẠI ĐÂY ===
        # Dùng `or 0` để chuyển None thành 0 trước khi gọi hàm max()
        mileage = max(row.get('mileage') or 0, 0)
        cycles = max(row.get('chargeCycles') or 0, 0)
        # ======================

        mileage_factor = 1 / np.log1p(mileage) if mileage > 0 else 1
        cycles_factor = 1 / np.log1p(cycles) if cycles > 0 else 1
        return (mileage_factor * 0.6 + cycles_factor * 0.4)
    return 1.0

# --- HÀM TẢI MODEL (ĐÃ SỬA) ---
def load_models():
    print("Đang tải các mô hình chuyên biệt...")
    
    # Bỏ biến 'success', chúng ta sẽ kiểm tra số lượng model tải được ở cuối
    for filename in set(MODEL_MAP.values()):
        if filename not in LOADED_MODELS:
            try:
                LOADED_MODELS[filename] = joblib.load(filename)
                print(f"✅ Tải mô hình {filename} thành công.")
            except FileNotFoundError:
                # SỬA ĐỔI: Chỉ in cảnh báo, không đánh dấu thất bại
                print(f"⚠️  Không tìm thấy {filename}. Service sẽ bỏ qua mô hình này.")
    
    # SỬA ĐỔI: Trả về True (thành công) miễn là có ÍT NHẤT MỘT model được tải
    return len(LOADED_MODELS) > 0

# --- HÀM DỰ ĐOÁN (ĐÃ SỬA LỖI) ---
def get_price_prediction(data):
    """
    Hàm này chứa TOÀN BỘ logic xử lý feature và dự đoán từ app.py.
    Nó nhận vào data (dict) và trả về (final_price, model_filename).
    """
    if not data:
        raise ValueError("Không nhận được dữ liệu (data is None)")
        
    product_type = str(data.get('productType', 'missing')).lower()
    
    # 1. Logic chọn mô hình dựa trên productType
    if product_type == 'car':
        brand = str(data.get('brand', 'missing'))
        brand_score = BRAND_MEAN_LOG_PRICES.get(brand, GLOBAL_BRAND_MEAN_LOG_PRICE)
        
        if brand_score <= CAR_LOG_THRESHOLD_LOW:
            model_filename = MODEL_MAP['car_low']
        else:
            model_filename = MODEL_MAP['car_high']
    else:
        model_filename = MODEL_MAP.get(product_type, MODEL_MAP['missing'])
        
    model = LOADED_MODELS.get(model_filename)

    if model is None:
        raise ValueError(f"Mô hình {model_filename} chưa được tải.")

    # 2. Trích xuất đặc trưng (Đồng bộ với train_model.py)
    # Tạo DataFrame từ dict data
    input_df = pd.DataFrame([data])

    input_df['productType'] = product_type
    
    # Dùng .get() an toàn cho các key có thể thiếu
    input_df['batteryCapacity_numeric'] = extract_capacity_value(data.get('batteryCapacity'))
    input_df['batteryLifespan_months'] = extract_lifespan_months(data.get('batteryLifespan'))
    input_df['chargeTime_numeric'] = extract_charge_time(data.get('chargeTime'))

    condition_id = data.get('conditionId')
    input_df['condition_score'] = (5 - condition_id) if condition_id in [1, 2, 3, 4] else 0

    year = data.get('yearOfManufacture')
    input_df['yearOfManufacture_numeric'] = pd.to_numeric(year, errors="coerce")
    
    # --- ĐÃ SỬA LỖI TẠI ĐÂY ---
    # Sử dụng np.where cho logic if/else trên cột
    # (pd.notna(...)) & (... > 1900)
    cond_valid_year = pd.notna(input_df['yearOfManufacture_numeric']) & (input_df['yearOfManufacture_numeric'] > 1900)
    
    input_df['age'] = np.where(
        cond_valid_year,                                    # Điều kiện (IF)
        CURRENT_YEAR - input_df['yearOfManufacture_numeric'], # Giá trị nếu True
        0                                                   # Giá trị nếu False
    )
    # --- KẾT THÚC SỬA LỖI ---

    # Đảm bảo các cột bắt buộc tồn tại (rất quan trọng)
    if 'mileage' not in input_df.columns: input_df['mileage'] = 0
    if 'chargeCycles' not in input_df.columns: input_df['chargeCycles'] = 0

    input_df['wear_score'] = input_df.apply(calculate_wear_score, axis=1)

    DEFAULT_MAX_SPEED = 50
    if 'maxSpeed' not in input_df.columns:
        input_df['maxSpeed'] = DEFAULT_MAX_SPEED
    else:
        # Xử lý cả 'None' và '0'
        input_df['maxSpeed'] = pd.to_numeric(input_df['maxSpeed'], errors='coerce').fillna(DEFAULT_MAX_SPEED)
        input_df['maxSpeed'] = input_df['maxSpeed'].replace(0, DEFAULT_MAX_SPEED) # Thay thế 0

    input_df['maxSpeed_safe'] = input_df['maxSpeed'].replace(0, 1e-6) # Tránh chia cho 0
    input_df["pin_value_per_speed"] = input_df["batteryCapacity_numeric"] / input_df['maxSpeed_safe']
    
    if 'brand' not in input_df.columns:
        input_df['brand'] = 'missing'
        
    input_df['brand'] = input_df['brand'].fillna("missing")
    input_df['brand_value_score'] = input_df['brand'].apply(
        lambda x: BRAND_MEAN_LOG_PRICES.get(x, GLOBAL_BRAND_MEAN_LOG_PRICE)
    )

    # 3. Dự đoán
    # (Pipeline trong model .pkl sẽ tự động chọn các cột nó cần)
    prediction = model.predict(input_df)
    predicted_price = np.expm1(prediction[0]) # Đây là numpy.float32

    # 4. Tránh giá âm
    MINIMUM_PRICE = 200_000
    final_price = max(MINIMUM_PRICE, predicted_price)

    # 5. Trả về INT và tên model
    return int(final_price), model_filename

# --- HÀM CALLBACK CỦA RABBITMQ (Gắn logic dự đoán vào) ---
def on_request(ch, method, properties, body):
    try:
        # 1. Nhận dữ liệu (dạng JSON string)
        request_data = json.loads(body.decode('utf-8'))
        print(f" [.] Received request: {request_data}")

        # 2. Gọi hàm dự đoán (đã bao gồm toàn bộ feature engineering)
        suggested_price, model_used = get_price_prediction(request_data)
        
        response_data = {
            'suggestedPrice': suggested_price,
            'model_used': model_used
        }
        print(f" [.] Predicted price: {suggested_price} (using {model_used})")

    except Exception as e:
        print(f" [!] Error processing request: {e}")
        traceback.print_exc() # In ra lỗi chi tiết
        response_data = {'suggestedPrice': 400000, 'error': str(e)} # Giá mặc định nếu lỗi

    # 3. Gửi Phản hồi (Reply)
    ch.basic_publish(
        exchange='',
        routing_key=properties.reply_to,
        properties=pika.BasicProperties(correlation_id=properties.correlation_id),
        body=json.dumps(response_data)
    )
    
    # 4. Báo cho RabbitMQ biết là đã xử lý xong
    ch.basic_ack(delivery_tag=method.delivery_tag)

# --- HÀM MAIN (ĐÃ SỬA) ---
def main():
    # 1. Tải model trước
    if not load_models():
        print("❌ Không tải được BẤT KỲ mô hình nào. Service không thể chạy. Thoát.")
        sys.exit(1)
    
    print(f"✅ Đã tải thành công {len(LOADED_MODELS)}/{len(set(MODEL_MAP.values()))} mô hình.")
    print(f"🚀 AI Service (MQ Consumer) đã sẵn sàng (năm {CURRENT_YEAR})")

    # 2. Thiết lập kết nối
    connection = None
    try:
        # === SỬA LỖI TẠI ĐÂY ===
        # Lấy host từ biến môi trường do Docker cung cấp
        # Nếu không có, mặc định là 'localhost' (dùng khi chạy ngoài Docker)
        rabbitmq_host = os.environ.get('RABBITMQ_HOST', 'localhost')
        # ======================

        connection = pika.BlockingConnection(pika.ConnectionParameters(host=rabbitmq_host))
        channel = connection.channel()

        # Khai báo Queue, THÊM durable=True để khớp với Java
        channel.queue_declare(queue=AI_REQUEST_QUEUE, durable=True)

        # Cân bằng tải: Chỉ nhận 1 tin nhắn mỗi lần
        channel.basic_qos(prefetch_count=1)
        
        # Đặt hàm on_request làm callback
        channel.basic_consume(queue=AI_REQUEST_QUEUE, on_message_callback=on_request)

        # === THÊM PRINT ĐỂ DEBUG ===
        print(f" [x] Awaiting RPC requests on '{AI_REQUEST_QUEUE}' (Connected to: {rabbitmq_host})")
        # ==========================

        channel.start_consuming()

    except pika.exceptions.AMQPConnectionError as e:
        # === THÊM PRINT ĐỂ DEBUG ===
        print(f"Error connecting to RabbitMQ at '{rabbitmq_host}': {e}")
        print(f"Please ensure RabbitMQ is running and accessible at {rabbitmq_host}")
        # ==========================
    except KeyboardInterrupt:
        print('Interrupted')
        if connection:
            connection.close()
    except Exception as e:
        print(f"An unexpected error occurred: {e}")
        traceback.print_exc()
    finally:
        if connection and connection.is_open:
            connection.close()

if __name__ == '__main__':
    main()