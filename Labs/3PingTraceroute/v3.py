import numpy as np
from scipy import stats

# Заданные данные
sample = np.array([
    0.679, 0.193, 0.550, 0.553, 0.388, 0.438, 0.205, 0.690, 0.770, 0.907,
    0.674, 0.275, 0.100, 0.471, 0.535, 0.111, 0.620, 0.243, 0.625, 0.523,
    0.459, 0.128, 0.501, 0.145, 0.743, 0.293, 0.432, 0.285, 0.611, 0.193
])

# Уровень значимости
epsilon = 0.13

# Проверка гипотезы о равномерном распределении U(0,1)
D, p_value = stats.kstest(sample, 'uniform', args=(0, 1))

print(f"Статистика Колмогорова-Смирнова: D = {D:.4f}")
print(f"p-значение: p = {p_value:.4f}")

# Сравнение p-значения с уровнем значимости
if p_value < epsilon:
    print(f"p-значение ({p_value:.6f}) < ε ({epsilon}) => отвергаем нулевую гипотезу")
else:
    print(f"p-значение ({p_value:.4f}) >= ε ({epsilon}) => нет оснований отвергать нулевую гипотезу")

# Альтернативный способ: сравнение статистики D с критическим значением
critical_value = stats.ksone.ppf(1 - epsilon/2, len(sample))
print(f"Критическое значение: D_crit = {critical_value:.4f}")

if D > critical_value:
    print(f"D ({D:.4f}) > D_crit ({critical_value:.4f}) => отвергаем нулевую гипотезу")
else:
    print(f"D ({D:.4f}) <= D_crit ({critical_value:.4f}) => нет оснований отвергать нулевую гипотезу")