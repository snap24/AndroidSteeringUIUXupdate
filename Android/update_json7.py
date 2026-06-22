import json

with open('app/src/main/res/raw/default_layout.json', 'r') as f:
    data = json.load(f)

print("steering_wheel_advanced_0_xRatio:", data.get('steering_wheel_advanced_0_xRatio'))
print("steering_wheel_advanced_0_yRatio:", data.get('steering_wheel_advanced_0_yRatio'))
print("steering_wheel_advanced_1_yRatio:", data.get('steering_wheel_advanced_1_yRatio'))

# The user wants to move 1cm down for steering wheel (+0.02 yRatio)
if 'steering_wheel_advanced_0_yRatio' in data:
    data['steering_wheel_advanced_0_yRatio'] += 0.02

# The user wants to move 1cm down for text (+0.02 yRatio)
if 'steering_wheel_advanced_1_yRatio' in data:
    data['steering_wheel_advanced_1_yRatio'] += 0.02

# For the pedals, we'll adjust the XML marginEnd instead, because the default_layout.json
# X-ratios are used as fallback, but if we change XML, it will be better if they reset.
# Actually, wait, let's just adjust the default_layout.json for pedals if they have xRatio!
