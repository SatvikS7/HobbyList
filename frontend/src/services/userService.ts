import { api } from './api';
import { type UserSummaryDto} from '../types';

export const userService = {
    getDiscoveryUsers: async (): Promise<UserSummaryDto[]> => {
        const response = await api.get<UserSummaryDto[]>('/users/discover');
        return response.data;
    },

}