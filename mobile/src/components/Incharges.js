import React from 'react';
import { View, Text } from 'react-native';
import InchargeList from '../container/InchargeList';
import getContainerStyle from '../utils/styleUtils';
import styles from '../styles/listsStyles';
import commonStyles from '../styles/commonStyles';

const { lightThemeText } = commonStyles;

const Incharges = () => (
  <View style={getContainerStyle()}>
    <Text style={[styles.title, lightThemeText]}>Selected Incharges</Text>
    <InchargeList selected />
  </View>
);

export default Incharges;
