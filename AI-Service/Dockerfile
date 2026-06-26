# STAGE 1: Cài đặt thư viện
# Sử dụng base image Python 3.10 loại mỏng (slim)
FROM python:3.10-slim

# Đặt thư mục làm việc bên trong container
WORKDIR /app

# Copy file requirements trước để tận dụng Docker cache
# Nếu file này không đổi, Docker sẽ không cần cài lại thư viện
COPY requirements.txt .

# Cài đặt các thư viện, không lưu cache để giữ image nhẹ
RUN pip install --no-cache-dir -r requirements.txt

# Copy toàn bộ mã nguồn và các model
# (Bao gồm app.py, và tất cả các file .pkl)
COPY app.py .
COPY *.pkl ./

# Khai báo biến môi trường cho RabbitMQ Host
# Giá trị 'localhost' là giá trị mặc định nếu không được cung cấp
ENV RABBITMQ_HOST=localhost

# Port mà service của bạn có thể expose (nếu có, ví dụ Flask)
# Nếu chỉ là Pika consumer thì không cần
# EXPOSE 5000

# Lệnh để chạy ứng dụng khi container khởi động
CMD ["python", "app.py"]