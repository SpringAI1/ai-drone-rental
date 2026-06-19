export const USE_MOCK = false

export const mockStats = {
  totalDrones: 10,
  totalUsers: 100,
  totalOrders: 500,
  totalRevenue: 150000
}

export const mockDrones = [
  {
    id: 1,
    model: 'DJI Mavic 3',
    brand: 'DJI',
    type: '消费级',
    image: '/static/drones/mavic3_drone.png',
    price: 299,
    deposit: 500,
    stock: 5,
    status: 1,
    description: '专业航拍无人机，搭载全画幅相机，支持4K视频录制，续航时间长达46分钟',
    specifications: {
      weight: '895g',
      maxSpeed: '19m/s',
      maxAltitude: '6000m',
      batteryLife: '46分钟'
    }
  },
  {
    id: 2,
    model: 'DJI Mini 3 Pro',
    brand: 'DJI',
    type: '消费级',
    image: '/static/drones/mini3pro_drone.png',
    price: 199,
    deposit: 300,
    stock: 8,
    status: 1,
    description: '轻巧便携无人机，仅重249g，无需注册，适合新手入门',
    specifications: {
      weight: '249g',
      maxSpeed: '16m/s',
      maxAltitude: '4000m',
      batteryLife: '34分钟'
    }
  },
  {
    id: 3,
    model: 'DJI Air 2S',
    brand: 'DJI',
    type: '消费级',
    image: '/static/drones/air2s_drone.png',
    price: 249,
    deposit: 400,
    stock: 6,
    status: 1,
    description: '全能航拍无人机，1英寸CMOS传感器，支持5.4K视频',
    specifications: {
      weight: '595g',
      maxSpeed: '19m/s',
      maxAltitude: '5000m',
      batteryLife: '31分钟'
    }
  },
  {
    id: 4,
    model: 'DJI Mavic 3 Pro',
    brand: 'DJI',
    type: '专业级',
    image: '/static/drones/mavic3pro_drone.png',
    price: 499,
    deposit: 1000,
    stock: 3,
    status: 1,
    description: '旗舰级无人机，三相机系统，专业影像创作利器',
    specifications: {
      weight: '958g',
      maxSpeed: '19m/s',
      maxAltitude: '6000m',
      batteryLife: '43分钟'
    }
  },
  {
    id: 5,
    model: 'DJI Mini 4 Pro',
    brand: 'DJI',
    type: '消费级',
    image: '/static/drones/mini4pro_drone.png',
    price: 229,
    deposit: 350,
    stock: 10,
    status: 1,
    description: '新一代迷你无人机，支持全向避障，智能跟随升级',
    specifications: {
      weight: '249g',
      maxSpeed: '16m/s',
      maxAltitude: '4000m',
      batteryLife: '34分钟'
    }
  }
]

export const mockOrders = [
  {
    id: 1,
    orderNo: 'ORD17816921141847B3F7A',
    userId: 1,
    username: '测试用户',
    droneId: 1,
    droneModel: 'DJI Mavic 3',
    droneImage: '/static/drones/mavic3_drone.png',
    rentalStartTime: '2026-06-20T00:00:00',
    rentalEndTime: '2026-06-23T23:59:59',
    rentalDays: 4,
    unitPrice: 299,
    totalAmount: 1196,
    depositAmount: 0,
    orderStatus: 0,
    orderStatusDesc: '待支付',
    hasComment: false,
    createdTime: '2026-06-17T18:28:34'
  },
  {
    id: 2,
    orderNo: 'ORD17816921141847B3F7B',
    userId: 1,
    username: '测试用户',
    droneId: 2,
    droneModel: 'DJI Mini 3 Pro',
    droneImage: '/static/drones/mini3pro_drone.png',
    rentalStartTime: '2026-06-21T00:00:00',
    rentalEndTime: '2026-06-25T23:59:59',
    rentalDays: 5,
    unitPrice: 199,
    totalAmount: 995,
    depositAmount: 0,
    orderStatus: 3,
    orderStatusDesc: '租赁中',
    hasComment: false,
    createdTime: '2026-06-18T10:00:00'
  },
  {
    id: 3,
    orderNo: 'ORD17816921141847B3F7C',
    userId: 1,
    username: '测试用户',
    droneId: 3,
    droneModel: 'DJI Air 2S',
    droneImage: '/static/drones/air2s_drone.png',
    rentalStartTime: '2026-06-15T00:00:00',
    rentalEndTime: '2026-06-18T23:59:59',
    rentalDays: 4,
    unitPrice: 249,
    totalAmount: 996,
    depositAmount: 0,
    orderStatus: 4,
    orderStatusDesc: '已归还',
    hasComment: true,
    createdTime: '2026-06-14T09:00:00'
  }
]

export const mockOrderStats = {
  pendingPay: 1,
  pendingShip: 0,
  pendingReceive: 0,
  renting: 1,
  returned: 1,
  canceled: 0
}

export const mockUserInfo = {
  id: 1,
  username: 'testuser',
  nickname: '测试用户',
  phone: '13800138000',
  email: 'test@example.com',
  avatar: '/static/drones/mavic3_drone.png',
  balance: 1000,
  creditScore: 100,
  role: 2,
  qualificationStatus: 2
}

export const mockLoginResult = {
  userId: 1,
  username: 'testuser',
  nickname: '测试用户',
  role: 2,
  token: 'mock_token_123456'
}

export const mockAirspaceRecords = [
  {
    id: 1,
    regionName: '北京朝阳区',
    regionRange: '北京市朝阳区奥林匹克公园',
    maxAltitude: 120,
    startTime: '2026-06-20 08:00:00',
    endTime: '2026-06-20 18:00:00',
    auditStatus: 1,
    auditRemark: null,
    createdTime: '2026-06-17T10:00:00'
  },
  {
    id: 2,
    regionName: '上海浦东新区',
    regionRange: '上海浦东国际机场周边区域',
    maxAltitude: 60,
    startTime: '2026-06-22 09:00:00',
    endTime: '2026-06-22 17:00:00',
    auditStatus: 0,
    auditRemark: null,
    createdTime: '2026-06-18T14:00:00'
  }
]

export const mockNotifications = [
  {
    id: 1,
    type: 1,
    title: '订单支付成功',
    content: '您的订单 ORD17816921141847B3F7A 已支付成功',
    readStatus: 0,
    createdTime: '2026-06-17T15:30:00'
  },
  {
    id: 2,
    type: 2,
    title: '空域备案通过',
    content: '您提交的北京朝阳区空域备案已通过审核',
    readStatus: 0,
    createdTime: '2026-06-17T16:00:00'
  }
]

export const mockFaultReports = [
  {
    id: 1,
    orderId: 2,
    orderNo: 'ORD17816921141847B3F7B',
    droneModel: 'DJI Mini 3 Pro',
    faultType: '电池故障',
    description: '电池无法正常充电，充电10分钟后自动断电',
    images: [],
    status: 0,
    createdTime: '2026-06-18T14:00:00'
  }
]

export const mockComments = [
  {
    id: 1,
    userId: 1,
    userNickname: '航拍爱好者',
    userAvatar: '/static/drones/mavic3_drone.png',
    content: '非常棒的无人机！拍摄效果一流，续航也很给力，强烈推荐！',
    rating: 5,
    images: [],
    children: [],
    createTime: '2026-06-17T15:30:00'
  },
  {
    id: 2,
    userId: 2,
    userNickname: '新手玩家',
    userAvatar: '/static/drones/mini3pro_drone.png',
    content: '操作简单，容易上手，适合新手使用',
    rating: 4,
    images: [],
    children: [],
    createTime: '2026-06-16T10:00:00'
  }
]

export const mockBrands = ['DJI', 'Autel', 'Parrot']
export const mockTypes = ['消费级', '专业级', '行业级']