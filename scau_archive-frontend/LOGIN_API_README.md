# 登录组件后端通信说明

## 功能概述

登录组件现在支持与后端API通信，包括：
- 发送用户名和密码到后端
- 处理登录响应
- 显示错误信息
- 加载状态管理

## API接口

### 登录接口
- **URL**: `POST /api/login`
- **请求体**:
  ```json
  {
    "username": "用户名",
    "password": "密码"
  }
  ```
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "登录成功",
    "token": "jwt-token",
    "user": {
      "id": 1,
      "username": "admin"
    }
  }
  ```
- **失败响应**:
  ```json
  {
    "success": false,
    "message": "用户名或密码错误"
  }
  ```

## 使用说明

1. **启动前端**: `npm run dev`
2. **启动后端**: 参考 `server-example.js` 创建后端服务
3. **测试登录**:
   - 用户名: `admin`, 密码: `123456`
   - 用户名: `user`, 密码: `password`

## 功能特性

- ✅ 前端表单验证
- ✅ 后端API调用
- ✅ 错误处理和显示
- ✅ 加载状态指示
- ✅ Token存储到localStorage
- ✅ 网络错误处理

## 注意事项

- 当前API地址硬编码为 `http://localhost:3000/api/login`
- 生产环境需要修改为实际的后端地址
- Token存储在localStorage中，实际项目中可能需要更安全的存储方式