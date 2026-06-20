export const USE_MOCK = true

export const mockStats = {
  totalDrones: 10,
  totalUsers: 100,
  totalOrders: 500,
  positiveRate: 98
}

export const mockDrones = [
  {
    id: 1,
    model: 'DJI Mavic 3',
    brand: 'DJI',
    type: '消费级',
    image: '/static/drones/mavic3_drone.png',
    pricePerDay: 299,
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
    model: 'DJI Mini 4 Pro',
    brand: 'DJI',
    type: '消费级',
    image: '/static/drones/mini4pro_drone.png',
    pricePerDay: 199,
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
    pricePerDay: 249,
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
    pricePerDay: 499,
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
    model: 'DJI Inspire 3',
    brand: 'DJI',
    type: '专业级',
    image: '/static/drones/mavic3_drone.png',
    pricePerDay: 899,
    price: 899,
    deposit: 2000,
    stock: 2,
    status: 1,
    description: '专业影视航拍无人机，全画幅8K，支持ProRes RAW录制',
    specifications: {
      weight: '3995g',
      maxSpeed: '29m/s',
      maxAltitude: '7000m',
      batteryLife: '28分钟'
    }
  },
  {
    id: 6,
    model: 'DJI Agras T40',
    brand: 'DJI',
    type: '行业级',
    image: '/static/drones/mavic3_drone.png',
    pricePerDay: 699,
    price: 699,
    deposit: 1500,
    stock: 4,
    status: 1,
    description: '农业植保无人机，40升超大载重，双电机冗余设计',
    specifications: {
      weight: '3800g',
      maxSpeed: '13m/s',
      maxAltitude: '3000m',
      batteryLife: '18分钟'
    }
  }
]

export const mockOrders = [
  {
    id: 1001,
    orderNo: 'DR20250601001',
    droneId: 1,
    droneName: 'DJI Mavic 3',
    droneBrand: 'DJI',
    droneImage: '/static/drones/mavic3_drone.png',
    amount: 598,
    deposit: 500,
    rentalDays: 2,
    startDate: '2025-06-01',
    endDate: '2025-06-03',
    status: 3,
    createTime: '2025-06-01 10:30:00',
    payTime: '2025-06-01 10:32:00'
  },
  {
    id: 1002,
    orderNo: 'DR20250603002',
    droneId: 2,
    droneName: 'DJI Mini 4 Pro',
    droneBrand: 'DJI',
    droneImage: '/static/drones/mini4pro_drone.png',
    amount: 398,
    deposit: 300,
    rentalDays: 2,
    startDate: '2025-06-03',
    endDate: '2025-06-05',
    status: 2,
    createTime: '2025-06-03 14:20:00',
    payTime: '2025-06-03 14:22:00'
  },
  {
    id: 1003,
    orderNo: 'DR20250605003',
    droneId: 3,
    droneName: 'DJI Air 2S',
    droneBrand: 'DJI',
    droneImage: '/static/drones/air2s_drone.png',
    amount: 747,
    deposit: 400,
    rentalDays: 3,
    startDate: '2025-06-05',
    endDate: '2025-06-08',
    status: 1,
    createTime: '2025-06-05 09:15:00'
  },
  {
    id: 1004,
    orderNo: 'DR20250510004',
    droneId: 4,
    droneName: 'DJI Mavic 3 Pro',
    droneBrand: 'DJI',
    droneImage: '/static/drones/mavic3pro_drone.png',
    amount: 998,
    deposit: 1000,
    rentalDays: 2,
    startDate: '2025-05-10',
    endDate: '2025-05-12',
    status: 4,
    createTime: '2025-05-10 11:00:00',
    payTime: '2025-05-10 11:02:00'
  },
  {
    id: 1005,
    orderNo: 'DR20250520005',
    droneId: 5,
    droneName: 'DJI Inspire 3',
    droneBrand: 'DJI',
    droneImage: '/static/drones/mavic3_drone.png',
    amount: 2697,
    deposit: 2000,
    rentalDays: 3,
    startDate: '2025-05-20',
    endDate: '2025-05-23',
    status: 6,
    createTime: '2025-05-19 16:30:00',
    cancelTime: '2025-05-19 17:00:00'
  }
]

export const mockOrderStats = {
  pendingPay: 2,
  pendingShip: 1,
  pendingReceive: 1,
  renting: 1,
  returned: 1,
  canceled: 1
}

export const mockUserInfo = {
  id: 1,
  username: 'demo',
  nickname: '飞行爱好者',
  phone: '138****8000',
  email: 'demo@airental.com',
  avatar: '/static/icons/user.png',
  balance: 1288,
  role: 0,
  qualificationStatus: 1
}

export const mockAirspaceRecords = [
  {
    id: 1,
    region: '北京·朝阳区',
    purpose: '航拍创作',
    area: '1500',
    startTime: '2025-06-10 09:00',
    endTime: '2025-06-10 18:00',
    status: 1,
    createTime: '2025-06-08 14:30:00'
  },
  {
    id: 2,
    region: '河北·张家口',
    purpose: '景区宣传',
    area: '3000',
    startTime: '2025-06-15 08:00',
    endTime: '2025-06-17 18:00',
    status: 0,
    createTime: '2025-06-10 10:00:00'
  }
]

export const mockNotifications = [
  {
    id: 1,
    type: 1,
    title: '订单即将到期',
    content: '您租借的 DJI Mavic 3 还有 24 小时到期，请及时归还或续租',
    readStatus: 0,
    createTime: '2025-06-18 14:30:00'
  },
  {
    id: 2,
    type: 5,
    title: '平台系统升级',
    content: '系统将于 6 月 25 日凌晨 2:00-4:00 进行升级维护，期间无法下单',
    readStatus: 0,
    createTime: '2025-06-17 20:00:00'
  },
  {
    id: 3,
    type: 2,
    title: '评价提醒',
    content: '您的订单 DR20250601001 已完成，期待您的真实评价帮助其他飞友',
    readStatus: 0,
    createTime: '2025-06-15 10:00:00'
  }
]

export const mockComments = [
  { id: 1, nickname: '飞友A', content: '机器很新，画质出色，客服响应及时！', rating: 5 },
  { id: 2, nickname: '摄影师小王', content: '4K视频效果惊艳，续航也够用，推荐！', rating: 5 },
  { id: 3, nickname: '航拍爱好者', content: '设备保养得不错，下次还会来租', rating: 4 }
]

export const mockBrands = ['DJI', 'Autel', 'Parrot', 'Yuneec']
export const mockTypes = ['消费级', '专业级', '行业级', '竞速机']
