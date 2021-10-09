import { createMuiTheme } from '@material-ui/core/styles';


export const theme = createMuiTheme ({
  overrides: {
    MuiPaper: {
      rounded: {
        borderRadius: '0.5rem',
      },
    },
  },
  typography: {
    button: {
      textTransform: 'none',
    },
  },
});
